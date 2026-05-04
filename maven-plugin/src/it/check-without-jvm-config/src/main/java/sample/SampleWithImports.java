// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package sample;

import java.util.List;

/**
 * A conforming sample that contains an import statement.
 *
 * <p>The import triggers palantir-java-format's {@code ImportOrderer.fixImports()}, which
 * internally accesses {@code com.sun.tools.javac.parser.Tokens$TokenKind}. This is the
 * exact internal JDK class that causes {@link IllegalAccessError} when
 * {@code jdk.compiler/com.sun.tools.javac.parser} is not opened to the unnamed module.
 * The test therefore verifies that {@code PalantirModuleOpener} has done its job.
 */
public class SampleWithImports {

    List<String> items;
}
