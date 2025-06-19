# CLIRunner – Command-Line Interface Component (Draft)

> **Draft Proposal**  
> This document outlines a preliminary sketch for adding command-line capabilities to the restructuring tool.  
> It is not a finalized design and should be refined during implementation.

## Purpose

Introduce a lightweight, console-invocable entry point that allows triggering the entire restructuring pipeline via standard terminal commands.  
This CLI component is aimed at:
- Supporting quick local testing and debugging
- Allowing integration into CI/CD and automation workflows
- Providing a developer-friendly way to validate or restructure files without needing to embed the tool into another Java application

## Responsibilities

- Parse CLI arguments
- Prepare and trigger configuration resolution
- Instantiate the main Processor and call either `restructure` or `check` flow
- Report outcome via terminal messages and exit codes

## Preliminary Argument Set (Subject to Change)

| Argument            | Description                                               |
|---------------------|-----------------------------------------------------------|
| `--mode=`           | Operation mode: `restructure` or `check`                  |
| `--input=`          | File or directory path to be processed                    |
| `--config=`         | Path to configuration file(s)                             |
| `--flags=`          | Optional override flags (e.g. `parser1:on,parser2:off`)   |

## Expected Exit Codes

| Code | Meaning                                          |
|------|--------------------------------------------------|
| 0    | Success (no changes needed or restructure done)  |
| 1    | Failure (e.g. check mode failed due to mismatch) |
| 2+   | Errors: invalid args, IO issues, internal crash  |

## Implementation Notes

- Prefer lightweight CLI parsers (e.g. `picocli`, `JCommander`) for ease of maintenance.
- It should stay thin: focus only on delegation, logging, and error handling.

## Note

> Further design will evolve once configuration bootstrapping and Processor contracts are finalized.
