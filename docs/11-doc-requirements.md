# Documentation Structure and Repository Layout

This section defines the expected structure of the documentation and repository layout for the project.
It is a non-functional but **mandatory** requirement aimed at ensuring clarity, consistency, and ease of collaboration
within the team.

## Repository Structure

```bash
my-project/
├── core/                  # Core utility
│   └── README-CORE.md     # Core documentation
├── maven-plugin/          # Maven plugin
│   └── README-MAVEN.md    # Plugin documentation
├── gradle-plugin/         # Gradle plugin (optional)
│   └── README-GRADLE.md
├── examples/              # Example setups
│   ├── jenkins/           # Jenkins example
│   │   └── EXAMPLE.md     # Description and usage
│   └── gitlab-ci.yml      # GitLab CI config
├── docs/                  # Global documentation
│   ├── CONFIG.md          # Configuration
│   ├── TROUBLESHOOTING.md
│   └── CHANGELOG.md
└── README.md              # Main entry point
```

## Required Markdown Documents

### `README.md` (root level)

Contains:
- Project description: _"Code formatting tool + plugins"_
- Module links:
  ```markdown
  - [Core Utility](/core/README-CORE.md)
  - [Maven Plugin](/maven-plugin/README-MAVEN.md)
  - [Examples](/examples/)
  ```
- Quick start:
  ```bash
  git clone https://github.com/your/project.git
  cd project
  mvn install
  ```
- Maven Central badge:
  [![Maven Central](https://img.shields.io/maven-central/v/com.your/plugin.svg)](https://search.maven.org)

---

### `/core/README-CORE.md`

Contains:
- CLI usage: `your-util format --target=src/`
- Configuration: `~/.your-util/config.yaml`
- Example pipeline:
  ```yaml
  steps:
    - name: Format code
      run: your-util format --check
  ```

---

### `/maven-plugin/README-MAVEN.md`

Contains:
- Usage in `pom.xml`:
  ```xml
  <plugin>
    <groupId>com.your</groupId>
    <artifactId>formatter-maven-plugin</artifactId>
    <version>1.0</version>
  </plugin>
  ```
- Execution: `mvn formatter:format`
- Parameters: `<skip>false</skip>`

---

### `/examples/EXAMPLE.md` (one per integration)

Example: Jenkins

```markdown
## Jenkins Integration

```groovy
pipeline {
  stages {
    stage('Format') {
      steps {
        withMaven(maven: 'maven-3.8') {
          sh 'mvn formatter:format'
        }
      }
    }
  }
}
```
```

---

### `/docs/CONFIG.md`

Contains:
- Shared configuration principles
- Environment variables:
  ```bash
  export FORMATTER_DEBUG=true
  ```
- Config formats: YAML / JSON

---

### `/docs/TROUBLESHOOTING.md`

| Error                    | Solution                                      |
|--------------------------|-----------------------------------------------|
| Unsupported Java version | Upgrade JDK to 17+                            |
| Config not found         | Run `your-util init` to create default config |

---

### `/docs/CHANGELOG.md`

Example entry:
```markdown
## [1.1.0] - 2025-06-20
### Added
- Java 21 support
```
