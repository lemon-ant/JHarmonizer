package io.github.lemon_ant.jharmonizer.core.flow;

import lombok.NonNull;

public enum FlowProcessingStatus {
    REORDERED,
    FORMATTED,
    CHECKED,
    UNCHANGED,
    ;

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
