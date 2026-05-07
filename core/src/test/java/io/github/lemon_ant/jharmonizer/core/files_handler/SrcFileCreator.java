// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.files_handler;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import java.nio.file.Path;
import lombok.NonNull;

public class SrcFileCreator {
    @NonNull
    public static SrcFile createSrcFile(@NonNull String srcCode, @NonNull Path path) {
        return new SrcFile(srcCode, path);
    }
}
