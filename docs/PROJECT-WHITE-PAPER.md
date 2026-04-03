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

## 💡 What Our Tool Will Do

### 🔧 Functionality

- Parses `.java` source files into an **AST** (Abstract Syntax Tree) using JavaParser or a similar library.
- Builds a **dependency graph for constants**, ensuring correct declaration order.
- Sorts class members based on configuration:
  - Grouping: `static final` constants, fields, constructors, getters/setters, methods.
  - Sorting inside groups: by **visibility** (public → private), optionally by type, name, annotations.
- Applies external formatter (e.g., **Palantir Java Format**) after sorting.

### 🧪 Supported Modes

- `sort`: reorder and rewrite files.
- `check`: validate correct order (for CI).
- `format`: apply external formatter only.
- `fix`: full sort + format in one pass.

---

## ⚙️ Configuration Sources

- Built-in default config.
- Exported IDE configs (e.g., `.editorconfig`, XML).
- Custom YAML config file (e.g., `sorter-config.yml`).
- Inline config via CLI or plugin params.

---

## 🧱 Integration Options

- CLI tool (local & CI use).
- Maven/Gradle plugin.
- Docker image.
- Optional: IDE plugin in the future.

---

## ✅ Why This Matters

- 💻 **IDE-independent**: works with any editor or environment.
- 🔒 Enables CI Quality Gates (e.g., GitLab).
- 📉 Reduces noisy diffs, improves PR readability.
- ⚙️ Fully automatable — no manual effort.
- 🌍 Can be released as open-source to represent **UBS engineering excellence**.
