// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.files_handler;

import java.nio.file.Path;
import lombok.NonNull;

public class SrcFileCreator {
    @NonNull
    public static SrcFile createSrcFile(@NonNull String srcCode, @NonNull Path path) {
        return new SrcFile(srcCode, path);
    }
}
