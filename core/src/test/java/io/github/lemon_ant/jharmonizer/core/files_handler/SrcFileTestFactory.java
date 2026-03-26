package io.github.lemon_ant.jharmonizer.core.files_handler;

import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SrcFileTestFactory {

    @NonNull
    public static SrcFile createSrcFile(@NonNull String srcCode, @NonNull Path path) {
        return new SrcFile(srcCode, path);
    }
}
