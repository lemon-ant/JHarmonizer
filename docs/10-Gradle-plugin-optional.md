# Gradle Plugin (Optional)

## Overview

While the core focus of this project is on delivering a reliable and tested Maven plugin for integration with Java
projects, we acknowledge the widespread use of Gradle in modern Java ecosystems. Therefore, we define the development
of a **Gradle plugin** as an **optional contribution**, outside the primary scope of the hackathon deliverables.

## Purpose

The Gradle plugin would mirror the functionality provided by the Maven plugin. It would allow Java projects using
Gradle to integrate the JHarmonizer tool directly into their build pipeline, enabling:

- Automatic reordering (`reorder`) of Java source code during the build process.
- Validation (`check`) of formatting consistency to prevent unformatted code from passing through CI/CD pipelines.
- Custom configuration passed through Gradle's extension and plugin DSL.

## Status

- **Optional**: The Gradle plugin is **not required** for the hackathon MVP.
- **Open for Contribution**: If team members experienced in Gradle development express interest, they are welcome
to take ownership of this component.
- **Testing Strategy**: Similar to Maven, the plugin should be validated through representative Gradle test projects
and integration testing.

## Key Requirements

- Provide Gradle DSL extension for configuration options (e.g. included paths, check/reorder mode, severity settings).
- Hook into appropriate Gradle build lifecycle phases (e.g. `compileJava` or earlier).
- Output diagnostics and reformatted code, or fail builds based on configurable thresholds.

## Contribution Notice

> If a contributor or a team member is passionate about Gradle and wishes to implement this plugin, we will fully
> support the effort. Otherwise, the plugin remains out of scope for initial development.
