<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Contributing to JHarmonizer

Thank you for your interest in contributing!
This document explains how to get the project running locally, what conventions to follow, and how the contribution workflow operates.

## Table of contents

- [Code of Conduct](#code-of-conduct)
- [Getting started](#getting-started)
- [Development environment](#development-environment)
- [Building and testing](#building-and-testing)
- [Creating issues](#creating-issues)
- [Creating pull requests](#creating-pull-requests)
- [Review process](#review-process)

---

## Code of Conduct

All participants are expected to uphold respectful and inclusive behavior toward other contributors.
Please read and follow the [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Getting started

External contributors work through a fork-based workflow, which is the standard approach for public
open-source projects on GitHub. Forking creates your own copy of the repository where you can freely
push branches without needing write access to the upstream repository.

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/JHarmonizer.git
   cd JHarmonizer
   ```
3. Add the upstream remote so you can sync with the main repository:
   ```bash
   git remote add upstream https://github.com/lemon-ant/JHarmonizer.git
   ```
4. Create a branch for your change:
   ```bash
   git checkout -b your-change-description
   ```

---

## Development environment

| Requirement | Version |
|-------------|---------|
| JDK         | 21 (required by Maven Enforcer) |
| Maven       | 3.6.3 or newer |

The simplest way to get the right JDK is via [SDKMAN!](https://sdkman.io/):

```bash
sdk install java 21.0.7-tem
```

No IDE-specific setup is required; any IDE that supports Maven projects works out of the box.
IntelliJ IDEA imports the project directly via the root `pom.xml`.

---

## Building and testing

Full build including all quality gates and tests:

```bash
mvn clean verify
```

Build without running tests:

```bash
mvn clean verify -DskipTests
```

All commands must be run from the repository root unless noted otherwise.

---

## Creating issues

Search existing issues before opening a new one to avoid duplicates.
When filing a bug, include the affected version, steps to reproduce, actual behavior, and expected behavior.
For feature requests, describe the use case and the expected behavior.

---

## Creating pull requests

1. Ensure your branch is up to date with `main`:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```
2. Run the full build locally and make sure it passes:
   ```bash
   mvn clean verify
   ```
3. Push your branch and open a pull request against `main` on GitHub.
4. Provide a clear description of the change, including the purpose, any testing performed, and any follow-up notes for reviewers.

Each pull request should:

- address a single concern (one bug fix, one feature, or one refactoring);
- include or update tests for any changed behavior;
- keep all quality gates green (Spotless, SortPOM, PMD, SpotBugs, JaCoCo);
- follow the conventions documented in [`AGENTS.md`](../AGENTS.md) (general code conventions) and `../docs/test-conventions.md` (test conventions, coming in a follow-up update);
- include SPDX metadata in every new file you add:
  ```
  SPDX-FileCopyrightText: <year> <Your Name> <your@email>
  SPDX-License-Identifier: Apache-2.0
  ```
  Use your own name and email for files you create.

---

## Review process

- All pull requests require at least one approving review from a maintainer.
- Automated CI checks (GitHub Actions) must pass before merging.
- Reviewers may request changes; please address the feedback and push additional commits.
- Once approved and CI is green, a maintainer will merge the pull request.
- Expect an initial review response within a few business days.

---

## Release process

The release process documentation will be added in a follow-up update.
