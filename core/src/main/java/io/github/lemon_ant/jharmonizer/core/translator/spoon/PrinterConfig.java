package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import lombok.Value;

/**
 * Immutable configuration controlling blank-line insertion by the source printer.
 * Instances are created once from the compiled configuration and passed to the printer.
 */
@Value
public class PrinterConfig {

    /**
     * Whether to insert a blank line after the type declaration header, before the first member.
     */
    boolean blankLineAfterTypeHeader;

    /**
     * Whether to insert a blank line before members with leading comments.
     */
    boolean blankLineBeforeComment;

    /**
     * Whether to insert a blank line between consecutive field declarations.
     */
    boolean blankLineBetweenFields;
}
