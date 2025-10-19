package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.EXACT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.REGEX;
import static java.util.Optional.ofNullable;

import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatcher;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

/**
 * Parses a single rule-line (set of tokens) into UnifiedMemberGroupRuleLine.
 */
@UtilityClass
class MemberGroupRuleLineParser {

    private static final Map<String, MemberKind> KIND_BY_TOKEN = TokenMaps.KIND_BY_TOKEN;
    private static final Map<String, MemberAccess> ACCESS_BY_TOKEN = TokenMaps.ACCESS_BY_TOKEN;
    private static final Map<String, DeclarationModifier> MOD_BY_TOKEN = TokenMaps.MOD_BY_TOKEN;

    static UnifiedMemberGroupRuleLine parse(Set<String> rawTokens) {
        Set<String> tokens = TokenNormalizer.normalizeTokens(rawTokens);

        UnifiedMemberGroupRuleLineBuilder ruleLineBuilder = UnifiedMemberGroupRuleLine.builder();
        for (String token : tokens) {
            if (handleNameToken(token, ruleLineBuilder)) {
                continue;
            }
            if (handleAnnotationToken(token, ruleLineBuilder)) {
                continue;
            }
            if (apply(ofNullable(KIND_BY_TOKEN.get(token)), ruleLineBuilder::memberKind)) {
                continue;
            }
            if (apply(ofNullable(ACCESS_BY_TOKEN.get(token)), ruleLineBuilder::memberAccess)) {
                continue;
            }
            apply(ofNullable(MOD_BY_TOKEN.get(token)), ruleLineBuilder::declarationModifier);
        }
        return ruleLineBuilder.build();
    }

    private static <T> boolean apply(Optional<T> opt, Consumer<T> applier) {
        opt.ifPresent(applier);
        return opt.isPresent();
    }

    private static boolean handleNameToken(String token, UnifiedMemberGroupRuleLineBuilder ruleLineBuilder) {
        if (token.startsWith("=")) {
            String value = token.substring(1).trim();
            if (!value.isEmpty()) {
                ruleLineBuilder.nameMatcher(new UnifiedNameMatcher(EXACT, value));
            }
            return true;
        }
        if (token.startsWith("~")) {
            String pattern = token.substring(1).trim();
            if (!pattern.isEmpty()) {
                ruleLineBuilder.nameMatcher(new UnifiedNameMatcher(REGEX, pattern));
            }
            return true;
        }
        return false;
    }

    private static boolean handleAnnotationToken(String token, UnifiedMemberGroupRuleLineBuilder ruleLineBuilder) {
        if (!token.startsWith("@")) return false;
        boolean isRegex = token.startsWith("@~");
        String body = token.substring(isRegex ? 2 : 1).trim();
        if (!body.isEmpty()) {
            ruleLineBuilder.annotationMatcher(new UnifiedAnnotationMatcher(isRegex ? REGEX : EXACT, body));
        }
        return true;
    }
}
