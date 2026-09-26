<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->
# Maven project root

Keep this directory in version control. Maven uses the nearest ancestor containing `.mvn` to set
`maven.multiModuleProjectDirectory`, including when launched from a module directory or with `-f core/pom.xml`.
The parent POM uses that property to locate shared license, PMD, and SpotBugs resources under `quality-gates/`.
Removing this directory breaks standalone module builds by resolving those paths against the module directory.

See [Maven configuration](https://maven.apache.org/configure.html).
