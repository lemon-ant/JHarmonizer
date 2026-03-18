package io.github.lemon_ant.jharmonizer.core.common;

import java.nio.file.Path;
import lombok.NonNull;
import lombok.Value;

@Value
public class SrcFile {
    @NonNull
    String srcCode;

    @NonNull
    Path path;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SrcFile that)) {
            return false;
        }
        return path.equals(that.path) && srcCode.equals(that.srcCode);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + srcCode.hashCode();
        return result;
    }
}
