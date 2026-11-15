// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/** Precomputed annotation ordering metadata, independent of source ranges and the AST implementation. */
@Value
public class AnnotationDescriptor {

    /**
     * Argument tokens inside parentheses, excluding comments and inter-token whitespace;
     * null if the annotation has no parentheses, empty if the parentheses contain no argument tokens.
     */
    @Nullable
    String arguments;

    /**
     * Written token/comment length, including Unicode escapes, excluding inter-token whitespace and incidental
     * text-block indentation.
     */
    int declarationLength;

    /** Simple annotation type name. */
    @NonNull
    String name;

    /**
     * Dependency keys contributed by the outer annotation and all visible nested annotations, at any depth.
     *
     * <p>Without source-declared repeatable metadata,
     * {@code @library.Tags({@library.Tag("z"), @library.Tag("a")})} contributes
     * {@code library.Tags} and {@code library.Tag}. A following {@code @library.Tag("b")} shares the latter key
     * and must remain after the whole container fragment; a single outer-type key would miss this dependency.
     * Source-declared repeatable relationships still normalize each type to its outermost container key:
     * a {@code Tag -> Tags -> TagGroups} chain needs one normalized type key, {@code TagGroups}.
     *
     * <p>A fragment can join independent sequences. Without source-declared repeatable relationships, for
     * {@code @Tag("first")}, {@code @Marker("first")}, then
     * {@code @Envelope(tags={@Tag("second")}, markers={@Marker("second")})}, conservative scanning preserves both
     * direct annotations before {@code Envelope}, but allows them to swap. Omitting qualification, their type keys are
     * {@code [Tag]}, {@code [Marker]}, and {@code [Envelope, Tag, Marker]}.
     * One universal key would also freeze the independent pair's order.
     *
     * <p>Repeated nested types contribute one key per fragment, preventing self-dependencies.
     * Key iteration order is irrelevant: dependencies follow the original source-fragment sequence.
     * Unresolved names retain lexical keys and add possible type/container group keys as ordering constraints.
     * For example, confirmed distinct {@code FirstOwner.Tag} and {@code SecondOwner.Tag} may both precede an
     * unresolved {@code UnknownOwner.Tag}. The unknown occurrence includes both confirmed keys as alternatives;
     * the confirmed occurrences retain disjoint key sets and remain independently sortable.
     * Alternative keys do not confirm an annotation identity or authorize declaration metadata lookup.
     */
    @NonNull
    Set<String> orderPreservationGroupKeys;

    /**
     * Retains a read-only view of the annotation's dependency keys without copying them.
     * Producers must not modify the supplied set after handoff.
     * @param arguments tokenized argument text, or null when parentheses are absent
     * @param declarationLength written length of declaration tokens and internal comments
     * @param name simple annotation type name
     * @param orderPreservationGroupKeys dependency keys linking fragments whose relative source order is preserved
     */
    public AnnotationDescriptor(
            @Nullable String arguments,
            int declarationLength,
            @NonNull String name,
            @NonNull Set<String> orderPreservationGroupKeys) {
        this.arguments = arguments;
        this.declarationLength = declarationLength;
        this.name = name;
        this.orderPreservationGroupKeys = Collections.unmodifiableSet(orderPreservationGroupKeys);
    }
}
