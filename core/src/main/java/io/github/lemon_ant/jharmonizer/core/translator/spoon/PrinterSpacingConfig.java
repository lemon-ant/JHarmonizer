package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import lombok.NonNull;
import lombok.Value;

/**
 * Immutable configuration controlling blank-line insertion by the source printer.
 * <p>
 * Instances are created once from the compiled configuration and passed to the printer
 * so that spacing predicates can be compiled at initialization time.
 */
@Value
public class PrinterSpacingConfig {

    /**
     * Whether to insert a blank line after the type declaration header, before the first member.
     */
    boolean blankLineAfterTypeHeader;

    /**
     * Whether to insert a blank line before annotated members.
     */
    boolean blankLineBeforeAnnotation;

    /**
     * Whether to insert a blank line before members with leading comments.
     */
    boolean blankLineBeforeComment;

    /**
     * Extracts a {@link PrinterSpacingConfig} from the formatting section of the compiled configuration.
     *
     * @param formatting the unified formatting settings
     * @return the extracted printer spacing config
     */
    @NonNull
    public static PrinterSpacingConfig fromFormatting(@NonNull UnifiedFormatting formatting) {
        return new PrinterSpacingConfig(
                formatting.isBlankLineAfterTypeHeader(),
                formatting.isBlankLineBeforeAnnotation(),
                formatting.isBlankLineBeforeComment());
    }
}
