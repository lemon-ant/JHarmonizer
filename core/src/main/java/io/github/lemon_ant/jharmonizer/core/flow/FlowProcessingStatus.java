package io.github.lemon_ant.jharmonizer.core.flow;

import lombok.NonNull;

/**
 * Outcome status of processing a single source file through a flow.
 * {@code REORDERED} means member order changed, {@code FORMATTED} means only formatting changed,
 * {@code CHECKED} means the file was verified and no changes were needed,
 * and {@code UNCHANGED} means the file was left as-is in a non-checking flow.
 */
public enum FlowProcessingStatus {
    REORDERED,
    FORMATTED,
    CHECKED,
    UNCHANGED,
    ;

    /**
     * Performs the define flow processing status.
     * @param hasRelocations the has relocations
     * @param contentChanged the content changed
     * @param checkingOnly the checking only
     * @return the result
     */
    @NonNull
    public static FlowProcessingStatus defineFlowProcessingStatus(
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
