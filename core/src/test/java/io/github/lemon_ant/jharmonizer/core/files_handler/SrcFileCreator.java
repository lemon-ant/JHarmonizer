package io.github.lemon_ant.jharmonizer.core.files_handler;

import lombok.NonNull;
import java.nio.file.Path;

public class SrcFileCreator {
    @NonNull
    public static SrcFile createSrcFile(@NonNull String srcCode, @NonNull Path path) {
        return new SrcFile(srcCode, path);
    }

}
