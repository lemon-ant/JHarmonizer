/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.flow;

import lombok.NonNull;

/**
 * Outcome status of processing a single source file through a flow.
 * {@code REORDERED} means member order changed, {@code FORMATTED} means only formatting changed,
 * {@code CHECKED} means the file was verified and no changes were needed,
 * {@code SKIPPED} means the file was intentionally skipped due to a file-level opt-out directive,
 * {@code UNCHANGED} means the file was left as-is in a non-checking flow,
 * and {@code ERROR} means processing failed due to an unexpected internal runtime error.
 */
public enum FileProcessingStatus {
    REORDERED,
    FORMATTED,
    CHECKED,
    SKIPPED,
    UNCHANGED,
    ERROR,
    ;

    /**
     * Resolves the resulting processing status from the observed relocation and formatting outcomes.
     *
     * @param hasRelocations whether member relocations were detected
     * @param contentChanged whether the produced source text differs from the original
     * @param checkingOnly whether the flow only validates input without rewriting files
     * @return the resulting processing status
     */
    @NonNull
    public static FileProcessingStatus defineFileProcessingStatus(
            boolean hasRelocations, boolean contentChanged, boolean checkingOnly) {
        if (hasRelocations) {
            return REORDERED;
        }
        if (contentChanged) {
            return FORMATTED;
        }
        if (checkingOnly) {
            return CHECKED;
        }
        return UNCHANGED;
    }
}
