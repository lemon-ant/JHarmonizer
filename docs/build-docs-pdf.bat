@echo off
REM === JReStructor PDF Build Script ===
REM Requires: pandoc + LaTeX (e.g., MiKTeX or TeX Live)

set OUTPUT=JReStructor-Full-Docs.pdf
set FILES=01-Intro.md 02-Configurator.md 03-Parser.md 04-Sorter.md 05-Formatter.md 06-Processor.md 07-DiffReporter.md 08-CliRunner.md 09-Maven-plugin.md 10-Gradle-plugin-optional.md 11-Tests.md 12-Documentation.md
::

echo Building %OUTPUT% ...
echo %FILES%

pandoc %FILES% ^
  -o "%OUTPUT%" ^
  --pdf-engine=xelatex ^
  -V mainfont="Arial" ^
  -V monofont="Consolas" ^
  -V geometry:margin=2cm ^
  --toc ^
  --number-sections ^
  --verbose
echo Done! Output file: %OUTPUT%
pause
