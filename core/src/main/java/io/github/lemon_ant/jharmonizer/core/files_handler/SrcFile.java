package io.github.lemon_ant.jharmonizer.core.files_handler;

import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SrcFile {
    @NonNull
    String srcCode;

    @NonNull
    Path path;
}
