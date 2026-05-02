<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# 📄 Java Code Reorderer - Project White Paper

## 🎯 Project Goal

Develop a **cross-platform, embeddable Java code formatter** that:

- ✅ Sorts class members (constants, fields, constructors, methods, etc.) according to a defined structure.
- 🧹 Formats code using an external formatter (e.g., Palantir Java Format).
- 🧰 Works independently of any IDE, CI platform, or editor.
- 🔌 Can be embedded as a CLI tool, Maven/Gradle plugin, Docker job, etc.

---

## 🧩 Why Existing Solutions Don’t Work

### ❌ JetBrains IDEA Code Style
- Works only inside IntelliJ IDEA.
- Requires manual formatting by the developer.
- Cannot be enforced via CI.
- Configuration is not portable across environments.

### ❌ Qodana
- Can validate member order only in the paid Ultimate edition.
- Does not auto-correct or sort code — just flags violations.
- Requires Docker, token management, and separate pipeline configuration.
- Not suitable for local development; overkill for simple CI enforcement.

### ❌ Google Java Format / Palantir Java Format
- Great for formatting code layout (indentation, spacing).
- ❗ Do not sort class members by structure or visibility.

### ❌ Spotless + Checkstyle
- Allow partial rule enforcement via custom checks.
- Do not perform actual reordering of code.
- Complex to configure, not scalable across teams.

---

## 💡 What Our Tool Does

### 🔧 Functionality

- Parses `.java` source files into an **AST** (Abstract Syntax Tree) using **Spoon**.
- Builds a per-type **declaration-order dependency graph** that captures the JLS rules
  for field initializers, initializer blocks, enum constant initializers, blank-final
  reads, and same-file cross-type back-references.
- Sorts class members based on configuration:
  - Grouping: `static final` constants, fields, constructors, getters/setters, methods.
  - Sorting inside groups: by **visibility** (public → private or vice versa), source
    position (`preserve`), and/or alphabetical name.
- Applies the **Palantir Java Format** (or Google/AOSP/none) after sorting and,
  optionally, fixes imports.

### 🧪 Supported Modes

- `reorder`: reorder and rewrite files (the "sort" / "fix" of older drafts).
- `check-all`: validate correct order; report every non-conforming file.
- `check-fast`: same as `check-all` but stops at the first non-conforming file.

---

## ⚙️ Configuration Sources

- Built-in default config (`default-config.yml`, embedded in `jharmonizer-core`).
- Custom YAML config file (path passed via CLI `--config` or Maven `<configFile>`).
- Selected per-run overrides via CLI options and Maven plugin parameters.

Importing other vendor configuration formats (IntelliJ IDEA `codeStyleConfig.xml`,
Eclipse formatter profiles, `.editorconfig`, Spotless config, ...) is on the
roadmap — see [`02-Configurator.md`](02-Configurator.md) for the layered model
that is being prepared for those adapters, and [`TODO.md`](TODO.md) for the
backlog entry tracking this work.

---

## 🧱 Integration Options

- CLI tool — `jharmonizer-cli` fat JAR (local & CI use).
- Maven plugin — `jharmonizer-maven-plugin` with goals `reorder`, `check-all`, `check-fast`.

A Gradle plugin and a Docker image are **not** shipped today; contributions are welcome.

---

## ✅ Why This Matters

- 💻 **IDE-independent**: works with any editor or environment.
- 🔒 Enables CI quality gates (e.g., GitLab, GitHub Actions).
- 📉 Reduces noisy diffs, improves PR readability.
- ⚙️ Fully automatable — no manual effort.
- 🌍 Released as open-source under Apache-2.0.
