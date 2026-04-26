<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Changelog

All notable changes to JHarmonizer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- Core pipeline: parse → classify → sort → print → format.
- Declaration-order dependency graph for direct initializer/read-write scenarios.
- Accessor bundling (`keepAccessorsTogether` option).
- Cycle detection with relaxed forward-reference fallback.
- Comment-based opt-out directives: `@jharmonizer:fully-off`, `@jharmonizer:sort-off` at file and type scope.
- Configurable member grouping and ordering via YAML/JSON configuration DSL.
- Maven plugin (`jharmonizer-maven-plugin`) with `reorder`, `check`, and `check-fast` goals.
- CLI fat JAR with `reorder`, `check-all`, and `check-fast` commands.
- JMH sorting performance benchmark (activate with `-Pbenchmark-sort`).
- Support for Maven archetype template files (graceful skip on non-Java placeholders).
- `CONTRIBUTING.md` — contributor guide covering fork/clone workflow, environment setup, build commands, code style, and review process.
- `CODE_OF_CONDUCT.md` — Contributor Covenant v2.1.
- `SECURITY.md` — vulnerability disclosure policy.
- `CHANGELOG.md` — this file.
- GitHub Actions CI workflow (`.github/workflows/ci.yml`) that builds and tests every push and pull request.
- GitHub Actions release workflow (`.github/workflows/release.yml`) for automated Maven Central publication on version tags.
- Issue templates for bug reports and feature requests.
- Pull request template.
- `CODEOWNERS` file for automatic review assignment.
- Dependabot configuration for automated dependency and GitHub Actions updates.
- Maven release profile with source JAR, Javadoc JAR, and GPG signing plugins.
- Maven POM metadata: `<description>`, `<url>`, `<scm>`, `<developers>`, `<issueManagement>`.

[Unreleased]: https://github.com/lemon-ant/JHarmonizer/commits/main
