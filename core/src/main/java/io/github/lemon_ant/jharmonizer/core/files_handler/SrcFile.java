package io.github.lemon_ant.jharmonizer.core.files_handler;

import java.nio.file.Path;
import lombok.NonNull;
import lombok.Value;

@Value
public class SrcFile {
    @NonNull
    String srcCode;

    @NonNull
    Path path;

    @NonNull
    public static SrcFile of(@NonNull String srcCode, @NonNull Path path) {
        return new SrcFile(srcCode, path);
    }

    SrcFile(@NonNull String srcCode, @NonNull Path path) {
        this.srcCode = srcCode;
        this.path = path;
    }
}
