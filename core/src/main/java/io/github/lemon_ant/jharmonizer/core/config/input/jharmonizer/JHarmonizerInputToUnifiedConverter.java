package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static io.github.lemon_ant.jharmonizer.core.config.unified.NameMatchKind.EXACT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.NameMatchKind.REGEX;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableSet;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.NameMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedRuleLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeGroup;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Converter from the package-private JHarmonizer input model to the *current* unified configuration model
 * (io.github.lemon_ant.jharmonizer.core.config.unified).
 * <p>
 * IMPORTANT:
 * - Lives in the same package as the input model to access package-private fields if needed.
 * - Uses builder API consistent with the unified classes (Sets with @Singular and .names(...) bulk setter).
 */
@Slf4j
@UtilityClass
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class JHarmonizerInputToUnifiedConverter {

    private static final Map<String, MemberKind> KIND_BY_TOKEN = Map.ofEntries(
            entry("field", MemberKind.FIELD),
            entry("method", MemberKind.METHOD),
            entry("constructor", MemberKind.CONSTRUCTOR),
            entry("init", MemberKind.INSTANCE_INIT_BLOCK),
            entry("initializer", MemberKind.INSTANCE_INIT_BLOCK),
            entry("static-init", MemberKind.STATIC_INIT_BLOCK),
            entry("static-initializer", MemberKind.STATIC_INIT_BLOCK),
            entry("class", MemberKind.TYPE_CLASS),
            entry("interface", MemberKind.TYPE_INTERFACE),
            entry("enum", MemberKind.TYPE_ENUM),
            entry("record", MemberKind.TYPE_RECORD),
            entry("annotation", MemberKind.TYPE_ANNOTATION));

    private static final Map<String, MemberAccess> ACCESS_BY_TOKEN = Map.ofEntries(
            entry("public", MemberAccess.PUBLIC),
            entry("protected", MemberAccess.PROTECTED),
            entry("package", MemberAccess.PACKAGE),
            entry("package-private", MemberAccess.PACKAGE),
            entry("private", MemberAccess.PRIVATE));

    private static final Map<String, DeclarationModifier> MOD_BY_TOKEN = Map.ofEntries(
            entry("static", DeclarationModifier.STATIC),
            entry("final", DeclarationModifier.FINAL),
            entry("abstract", DeclarationModifier.ABSTRACT),
            entry("synchronized", DeclarationModifier.SYNCHRONIZED),
            entry("native", DeclarationModifier.NATIVE),
            entry("transient", DeclarationModifier.TRANSIENT),
            entry("volatile", DeclarationModifier.VOLATILE),
            entry("strictfp", DeclarationModifier.STRICTFP),
            entry("default", DeclarationModifier.DEFAULT),
            entry("sealed", DeclarationModifier.SEALED),
            entry("non-sealed", DeclarationModifier.NON_SEALED),
            entry("nonsealed", DeclarationModifier.NON_SEALED));

    private static <T> boolean apply(Optional<T> opt, Consumer<T> applier) {
        opt.ifPresent(applier);
        return opt.isPresent();
    }

    private static Optional<MemberKind> mapKind(String token) {
        return ofNullable(KIND_BY_TOKEN.get(token));
    }

    private static Optional<MemberAccess> mapAccess(String token) {
        return ofNullable(ACCESS_BY_TOKEN.get(token));
    }

    private static Optional<DeclarationModifier> mapModifier(String token) {
        return ofNullable(MOD_BY_TOKEN.get(token));
    }

    private static UnifiedTopLevelTypesOrdering mapTopLevelTypesOrdering(TopLevelTypesOrdering topLevelTypesOrdering) {
        List<UnifiedTypeGroup> typeGroups = topLevelTypesOrdering.getTypeGroups().stream()
                .map(typeGroup -> new UnifiedTypeGroup(typeGroup.getTypeKinds().stream()
                        .map(TypeKind::getUnifiedTypeKind)
                        .collect(toUnmodifiableSet())))
                .toList();

        List<UnifiedSortKey> sortKeys = topLevelTypesOrdering.getSortKeys().stream()
                .map(SortKey::getUnifiedSortKey)
                .toList();

        return UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(topLevelTypesOrdering.isMainTypeFirst())
                .typeGroups(typeGroups)
                .sortKeys(sortKeys)
                .build();
    }

    public static UnifiedConfig convert2Unified(@NonNull JHarmonizerConfig jHarmonizerConfig) {
        // 1) Top-level types ordering
        UnifiedTopLevelTypesOrdering topLevelTypesOrdering =
                mapTopLevelTypesOrdering(jHarmonizerConfig.getTopLevelTypesOrdering());

        // 2) fixImports
        boolean fixImports = jHarmonizerConfig.isFixImports();

        // 3) formatterStyle
        UnifiedFormatterStyle formatterStyle =
                jHarmonizerConfig.getFormatterStyle().getUnifiedFormatterStyle();

        // 4) headerLine
        UnifiedHeaderLine headerLine = new UnifiedHeaderLine(
                jHarmonizerConfig.getHeaderLine().getCharacter(),
                jHarmonizerConfig.getHeaderLine().getLeftPadding());

        // 5) root member groups
        List<UnifiedMemberGroup> unifiedMemberGroups = jHarmonizerConfig.getMemberGroups().stream()
                .map(JHarmonizerInputToUnifiedConverter::convertMemberGroup)
                .toList();

        return UnifiedConfig.builder()
                .topLevelTypesOrdering(topLevelTypesOrdering)
                .fixImports(fixImports)
                .formatterStyle(formatterStyle)
                .headerLine(headerLine)
                .rootMemberGroups(unifiedMemberGroups)
                .build();
    }

    private static UnifiedMemberGroup convertMemberGroup(@NonNull MemberGroup memberGroup) {
        UnifiedSelectorBlock.UnifiedSelectorBlockBuilder selectorBlockBuilder = UnifiedSelectorBlock.builder();
        memberGroup.getIncludes().stream()
                .map(JHarmonizerInputToUnifiedConverter::parseRuleLine)
                .forEach(selectorBlockBuilder::include);
        memberGroup.getExcludes().stream()
                .map(JHarmonizerInputToUnifiedConverter::parseRuleLine)
                .forEach(selectorBlockBuilder::exclude);
        UnifiedSelectorBlock selectorBlock = selectorBlockBuilder.build();

        List<UnifiedSortKey> unifiedSortKeys = memberGroup.getSortKeys().stream()
                .map(SortKey::getUnifiedSortKey)
                .toList();

        UnifiedSortingBehavior sortingBehavior = UnifiedSortingBehavior.builder()
                .unifiedSortKeys(unifiedSortKeys)
                .keepAccessorsTogether(memberGroup.isKeepAccessorsTogether())
                .build();

        UnifiedMemberGroup.UnifiedMemberGroupBuilder memberGroupBuilder = UnifiedMemberGroup.builder()
                .groupName(memberGroup.getName())
                .selectorBlock(selectorBlock)
                .sortingBehavior(sortingBehavior);

        memberGroup.getMemberSubGroups().stream()
                .map(JHarmonizerInputToUnifiedConverter::convertMemberGroup)
                .forEach(memberGroupBuilder::memberSubGroup);
        return memberGroupBuilder.build();
    }

    private static UnifiedRuleLine parseRuleLine(@NonNull Set<String> rawTokens) {
        Set<String> tokens = normalizeTokens(rawTokens);

        UnifiedRuleLine.UnifiedRuleLineBuilder b = UnifiedRuleLine.builder();
        for (String token : tokens) {
            if (handleNameToken(token, b)) continue;
            if (handleAnnotationToken(token, b)) continue;
            if (apply(mapKind(token), b::kind)) continue;
            if (apply(mapAccess(token), b::accessLevel)) continue;
            apply(mapModifier(token), b::declarationModifier);
        }
        return b.build();
    }

    private static Set<String> normalizeTokens(@NonNull Set<String> rawTokens) {
        return rawTokens.stream()
                .map(StringUtils::trimToNull)
                .filter(rawToken -> {
                    if (rawToken == null) {
                        log.warn("An empty token was found in line {}", rawTokens);
                        return false;
                    } else {
                        return true;
                    }
                })
                .map(token -> token.toLowerCase(Locale.ENGLISH))
                .collect(toUnmodifiableSet());
    }

    private static boolean handleNameToken(String token, UnifiedRuleLine.UnifiedRuleLineBuilder b) {
        if (token.startsWith("=")) {
            String value = token.substring(1).trim();
            if (!value.isEmpty()) {
                b.name(new NameMatcher(EXACT, value));
            }
            return true;
        }
        if (token.startsWith("~")) {
            String pattern = token.substring(1).trim();
            if (!pattern.isEmpty()) {
                b.name(new NameMatcher(REGEX, pattern));
            }
            return true;
        }
        return false;
    }

    private static boolean handleAnnotationToken(String token, UnifiedRuleLine.UnifiedRuleLineBuilder b) {
        if (!token.startsWith("@")) return false;
        boolean isRegex = token.startsWith("@~");
        String body = token.substring(isRegex ? 2 : 1).trim();
        if (!body.isEmpty()) {
            b.annotation(new AnnotationMatcher(isRegex ? REGEX : EXACT, body));
        }
        return true;
    }
}
