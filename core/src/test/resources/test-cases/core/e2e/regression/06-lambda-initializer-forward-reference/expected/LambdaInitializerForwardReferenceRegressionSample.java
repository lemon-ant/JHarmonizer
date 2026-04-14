package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

class LambdaInitializerForwardReferenceRegressionSample {
    private static final Pattern ENTRY_PATTERN = Pattern.compile("entry-(.*)");
    private static final FileFilter ENTRY_DIRECTORY_FILTER =
            f -> ENTRY_PATTERN.matcher(f.getName()).matches();

    public static void main(String[] args) {
        if (!ENTRY_DIRECTORY_FILTER.accept(new File("entry-foo"))
                || ENTRY_DIRECTORY_FILTER.accept(new File("other-foo"))) {
            throw new IllegalStateException("Unexpected filter behavior:"
                    + " entry-foo=" + ENTRY_DIRECTORY_FILTER.accept(new File("entry-foo"))
                    + ", other-foo=" + ENTRY_DIRECTORY_FILTER.accept(new File("other-foo")));
        }
    }
}
