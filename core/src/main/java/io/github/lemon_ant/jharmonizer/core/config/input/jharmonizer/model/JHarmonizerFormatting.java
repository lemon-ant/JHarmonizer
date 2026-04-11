package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

/**
 * Formatting section of a JHarmonizer YAML config, controlling import fixing, formatter style,
 * and printer options.
 */
@Value
public class JHarmonizerFormatting {

    // TODO Exclude to a dedicated class
    boolean fixImports;

    @NonNull
    // TODO Exclude to a dedicated class
    FormatterStyle formatterStyle;

    boolean blankLineAfterTypeHeader;

    boolean blankLineBeforeAnnotation;

    boolean blankLineBeforeComment;

    /**
     * Creates a new JHarmonizerFormatting.
     *
     * @param fixImports whether to fix/reorder imports
     * @param formatterStyle the formatter style
     * @param blankLineAfterTypeHeader whether to add a blank line after type header before the first member
     * @param blankLineBeforeAnnotation whether to add a blank line before annotated members
     * @param blankLineBeforeComment whether to add a blank line before members with leading comments
     */
    public JHarmonizerFormatting(
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle,
            @JsonProperty(value = "blank-line-after-type-header", required = true) boolean blankLineAfterTypeHeader,
            @JsonProperty(value = "blank-line-before-annotation", required = true) boolean blankLineBeforeAnnotation,
            @JsonProperty(value = "blank-line-before-comment", required = true) boolean blankLineBeforeComment) {
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.blankLineAfterTypeHeader = blankLineAfterTypeHeader;
        this.blankLineBeforeAnnotation = blankLineBeforeAnnotation;
        this.blankLineBeforeComment = blankLineBeforeComment;
    }
}
