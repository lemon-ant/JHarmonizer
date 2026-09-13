// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathDirectoryUrl;

import java.net.URI;
import java.nio.file.Path;
import lombok.NonNull;

class SrcProcessorE2EFixtureTest extends AbstractCompilableSrcProcessorE2ETest {

    @NonNull
    private static final Path FIXTURES_ROOT = Path.of(URI.create(
            requireClasspathDirectoryUrl("/test-cases/core/e2e/reorder/").toExternalForm()));

    @NonNull
    @Override
    protected Path getFixturesRoot() {
        return FIXTURES_ROOT;
    }
}
