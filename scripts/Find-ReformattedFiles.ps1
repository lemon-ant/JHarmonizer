# SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
# SPDX-License-Identifier: Apache-2.0

<#
.SYNOPSIS
    Runs JHarmonizer a second time and collects the full paths of files that were
    re-formatted (non-idempotent formatter output).

.DESCRIPTION
    On a second run, a deterministic formatter should not change any file.
    This script runs JHarmonizer with -v / --verbose so that DEBUG log lines are
    emitted, then:
      1. Parses "Emitting <full-path>" lines (from GlobPathFinder) to build a map of
         every processed file's full absolute path.
      2. Parses "JHarmonizer FORMATTED <path>" lines (from SrcProcessor) to collect
         files that were still being re-formatted.  The logged path is abbreviated to
         at most 100 characters (e.g. "...\db\JdbcCommon.java"), so step 1 is needed
         to resolve the original full path by matching the trailing path suffix.
      3. Writes the resolved full paths to a plain text file (one path per line).

    You can then restore those files to their pre-first-run state and copy them to a
    separate folder for analysis or issue filing.

.PARAMETER JarPath
    Absolute or relative path to the JHarmonizer CLI JAR.
    Example: C:\tools\jharmonizer-cli.jar

.PARAMETER SubCommand
    The JHarmonizer sub-command to run.  Accepted values: reorder, check-all,
    check-fail-fast.  Default: reorder.

.PARAMETER BaseDir
    Value passed as -b / --base-dir to JHarmonizer.  Defaults to the current
    directory when not specified.

.PARAMETER ConfigFile
    Optional path to a custom YAML config file (-c / --config).

.PARAMETER Include
    Optional glob patterns to pass as --include (repeat the parameter or
    comma-separate patterns in a single string).

.PARAMETER Exclude
    Optional glob patterns to pass as --exclude.

.PARAMETER NoBackup
    When set, passes --no-backup to JHarmonizer.

.PARAMETER NoStatistics
    When set, passes --no-statistics to JHarmonizer.

.PARAMETER JavaExe
    Path to the java executable.  Defaults to "java" (must be on PATH).

.PARAMETER OutputFile
    Path to the output text file that will list all re-formatted files.
    Default: reformatted-files.txt in the current directory.

.EXAMPLE
    .\Find-ReformattedFiles.ps1 `
        -JarPath    C:\tools\jharmonizer-cli.jar `
        -SubCommand reorder `
        -BaseDir    W:\nifi `
        -OutputFile C:\tmp\reformatted.txt

.EXAMPLE
    .\Find-ReformattedFiles.ps1 `
        -JarPath   C:\tools\jharmonizer-cli.jar `
        -BaseDir   W:\nifi `
        -Include   "**/*.java" `
        -Exclude   "**/generated/**" `
        -NoBackup
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $JarPath,

    [ValidateSet('reorder', 'check-all', 'check-fail-fast')]
    [string] $SubCommand = 'reorder',

    [string] $BaseDir = '.',

    [string] $ConfigFile,

    [string[]] $Include = @(),

    [string[]] $Exclude = @(),

    [switch] $NoBackup,

    [switch] $NoStatistics,

    [string] $JavaExe = 'java',

    [string] $OutputFile = 'reformatted-files.txt'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Build the java argument list
# ---------------------------------------------------------------------------
$javaArgs = [System.Collections.Generic.List[string]]::new()
$javaArgs.Add('-jar')
$javaArgs.Add($JarPath)
$javaArgs.Add($SubCommand)
$javaArgs.Add('--base-dir')
$javaArgs.Add($BaseDir)

# Always enable verbose so we get DEBUG-level "Emitting" and "FORMATTED" lines.
$javaArgs.Add('--verbose')

if ($ConfigFile) {
    $javaArgs.Add('--config')
    $javaArgs.Add($ConfigFile)
}
foreach ($pattern in $Include) {
    $javaArgs.Add('--include')
    $javaArgs.Add($pattern)
}
foreach ($pattern in $Exclude) {
    $javaArgs.Add('--exclude')
    $javaArgs.Add($pattern)
}
if ($NoBackup)     { $javaArgs.Add('--no-backup') }
if ($NoStatistics) { $javaArgs.Add('--no-statistics') }

Write-Host ''
Write-Host "Running JHarmonizer (second pass, verbose logging)..."
Write-Host "  $JavaExe $($javaArgs -join ' ')"
Write-Host ''

# ---------------------------------------------------------------------------
# Execute and capture all output
# ---------------------------------------------------------------------------
# JHarmonizer logs to stdout via a Logback ConsoleAppender; JVM warnings may
# arrive on stderr.  Redirect stderr to the success stream so we capture both.
$rawLines = & $JavaExe @javaArgs 2>&1

# ---------------------------------------------------------------------------
# Parse log lines
# ---------------------------------------------------------------------------
# Verbose log pattern (VERBOSE_LOG_PATTERN in BaseCommand.java):
#   %-5level [%-8.8thread] [%logger{36}] %msg%n
#
# Lines of interest:
#   DEBUG [main    ] [i.g.l.globpathfinder.GlobPathFinder] Emitting W:\path\File.java
#   DEBUG [main    ] [i.g.l.jharmonizer.core.SrcProcessor] JHarmonizer FORMATTED ...\path\File.java

$emittedPaths     = [System.Collections.Generic.List[string]]::new()
$formattedAbbrevs = [System.Collections.Generic.List[string]]::new()

foreach ($entry in $rawLines) {
    # $entry is a string for stdout lines and an ErrorRecord for stderr lines.
    $text = "$entry"

    if ($text -match '\]\s+Emitting (.+)$') {
        $emittedPaths.Add($Matches[1].Trim())
        continue
    }
    if ($text -match '\]\s+JHarmonizer FORMATTED (.+)$') {
        $formattedAbbrevs.Add($Matches[1].Trim())
    }
}

Write-Host "Total files processed : $($emittedPaths.Count)"
Write-Host "Files re-formatted    : $($formattedAbbrevs.Count)"
Write-Host ''

if ($formattedAbbrevs.Count -eq 0) {
    Write-Host 'No re-formatted files found — formatter output is deterministic. Nothing to save.' `
        -ForegroundColor Green
    exit 0
}

# ---------------------------------------------------------------------------
# Resolve abbreviated paths to full paths
# ---------------------------------------------------------------------------
# SrcProcessor abbreviates paths longer than 100 characters as:
#   "...<sep><tail-segments>"  e.g. "...\util\db\JdbcCommon.java"
# The tail is built by keeping as many trailing path segments as fit, so it is
# a unique-enough suffix to identify the file among the emitted full paths.

$resolvedPaths  = [System.Collections.Generic.List[string]]::new()
$unresolvedList = [System.Collections.Generic.List[string]]::new()

foreach ($abbrev in $formattedAbbrevs) {
    if (-not $abbrev.StartsWith('...')) {
        # Path was short enough to be logged without abbreviation.
        $resolvedPaths.Add($abbrev)
        continue
    }

    # Strip the leading "...\" or ".../" and normalise separator to backslash.
    $suffix     = ($abbrev -replace '^\.\.\.[\\/]', '').Replace('/', '\')
    $candidates = @(
        $emittedPaths | Where-Object {
            $_.Replace('/', '\').EndsWith('\' + $suffix) -or
            $_.Replace('/', '\') -eq $suffix
        }
    )

    if ($candidates.Count -eq 0) {
        Write-Warning "Cannot resolve abbreviated path: $abbrev"
        $unresolvedList.Add($abbrev)
    } elseif ($candidates.Count -gt 1) {
        $first = $candidates[0]
        Write-Warning "Ambiguous match for '$abbrev' ($($candidates.Count) candidates); using: $first"
        $resolvedPaths.Add($first)
    } else {
        $resolvedPaths.Add($candidates[0])
    }
}

# ---------------------------------------------------------------------------
# Save results
# ---------------------------------------------------------------------------
$resolvedPaths | Set-Content -Encoding UTF8 -Path $OutputFile

Write-Host "Saved $($resolvedPaths.Count) path(s) to: $(Resolve-Path $OutputFile)" `
    -ForegroundColor Yellow

if ($unresolvedList.Count -gt 0) {
    Write-Host ''
    Write-Host "WARNING: $($unresolvedList.Count) abbreviated path(s) could not be resolved and were omitted:" `
        -ForegroundColor DarkYellow
    foreach ($p in $unresolvedList) {
        Write-Host "  - $p" -ForegroundColor DarkYellow
    }
}
