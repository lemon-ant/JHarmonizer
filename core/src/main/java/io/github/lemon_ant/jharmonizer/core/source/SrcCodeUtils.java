package io.github.lemon_ant.jharmonizer.core.source;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Provides helpers for working with raw source code text.
 */
@UtilityClass
public class SrcCodeUtils {

    /**
     * Finds the first indentation character position for the source fragment.
     *
     * @param start the source index to scan backward from
     * @param srcCode the source code text to inspect
     * @return the indentation start offset for the fragment
     */
    public static int findIndentationStart(int start, @NonNull String srcCode) {
        int position = start - 1;
        while (position >= 0) {
            char character = srcCode.charAt(position);
            if (character != '\t' && character != ' ') {
                break;
            }
            position--;
        }
        return position + 1;
    }
}
