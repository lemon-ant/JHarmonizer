package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Compiles a list of {@link UnifiedOrderingRule} values into the corresponding
 * compiled {@link OrderingRule} values used during member sorting.
 */
@UtilityClass
class OrderingRuleCompiler {

    @NonNull
    static List<OrderingRule> compileOrderingRules(@NonNull List<UnifiedOrderingRule> unifiedOrderingRules) {
        // Unified model guarantees non-empty list; preserve order and map 1:1.
        return unifiedOrderingRules.stream()
                .map(OrderingRuleCompiler::compileOrderingRule)
                .toList();
    }

    @NonNull
    private static OrderingRule compileOrderingRule(@NonNull UnifiedOrderingRule unifiedOrderingRule) {
        return switch (unifiedOrderingRule) {
            case ALPHA -> OrderingRule.ALPHA;
            case PRESERVE -> OrderingRule.PRESERVE;
            case VISIBILITY_ASC -> OrderingRule.VISIBILITY_ASC;
            case VISIBILITY_DESC -> OrderingRule.VISIBILITY_DESC;
        };
    }
}
