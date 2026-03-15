package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigLoader;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFormatting;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JHarmonizer2FlexibleUnifiedConverter {

    private static final String FIELD_TOP_LEVEL_TYPES_ORDERING = "top-level-types-ordering";
    private static final String FIELD_FORMATTING = "formatting";
    private static final String FIELD_BACKUPS_ENABLED = "backups-enabled";
    private static final String FIELD_HEADER_LINE = "header-line";
    private static final String FIELD_TYPE_MEMBERS_ORDERING = "type-members-ordering";

    @NonNull
    public static FlexibleUnifiedConfig convert2Flexible(@NonNull JsonNode vendorConfigRoot) {
        UnifiedTopLevelTypesOrdering topLevelTypesOrdering = readTopLevelTypesOrdering(vendorConfigRoot);
        UnifiedFormatting formatting = readFormatting(vendorConfigRoot);
        Boolean backupsEnabled = readBackupsEnabled(vendorConfigRoot);
        UnifiedHeaderLine headerLine = readHeaderLine(vendorConfigRoot);
        List<UnifiedMemberGroup> rootMemberGroups = readRootMemberGroups(vendorConfigRoot);
        return new FlexibleUnifiedConfig(
                topLevelTypesOrdering, formatting, backupsEnabled, headerLine, rootMemberGroups);
    }

    private static UnifiedTopLevelTypesOrdering readTopLevelTypesOrdering(JsonNode vendorConfigRoot) {
        JsonNode fieldNode = vendorConfigRoot.get(FIELD_TOP_LEVEL_TYPES_ORDERING);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        JHarmonizerTopLevelTypesOrdering vendorTopLevelTypesOrdering =
                JHarmonizerConfigLoader.treeToValue(fieldNode, JHarmonizerTopLevelTypesOrdering.class);
        return TopLevelTypesOrderingMapper.map(vendorTopLevelTypesOrdering);
    }

    private static UnifiedFormatting readFormatting(JsonNode vendorConfigRoot) {
        JsonNode fieldNode = vendorConfigRoot.get(FIELD_FORMATTING);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        JHarmonizerFormatting vendorFormatting =
                JHarmonizerConfigLoader.treeToValue(fieldNode, JHarmonizerFormatting.class);
        return new UnifiedFormatting(
                vendorFormatting.isFixImports(),
                vendorFormatting.getFormatterStyle().getUnifiedFormatterStyle());
    }

    private static Boolean readBackupsEnabled(JsonNode vendorConfigRoot) {
        JsonNode fieldNode = vendorConfigRoot.get(FIELD_BACKUPS_ENABLED);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return JHarmonizerConfigLoader.treeToValue(fieldNode, Boolean.class);
    }

    private static UnifiedHeaderLine readHeaderLine(JsonNode vendorConfigRoot) {
        JsonNode fieldNode = vendorConfigRoot.get(FIELD_HEADER_LINE);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        JHarmonizerHeaderLine vendorHeaderLine =
                JHarmonizerConfigLoader.treeToValue(fieldNode, JHarmonizerHeaderLine.class);
        return new UnifiedHeaderLine(vendorHeaderLine.getCharacter(), vendorHeaderLine.getLeftPadding());
    }

    private static List<UnifiedMemberGroup> readRootMemberGroups(JsonNode vendorConfigRoot) {
        JsonNode fieldNode = vendorConfigRoot.get(FIELD_TYPE_MEMBERS_ORDERING);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        List<JHarmonizerMemberGroup> vendorMemberGroups =
                JHarmonizerConfigLoader.treeToValue(fieldNode, new TypeReference<List<JHarmonizerMemberGroup>>() {});
        return vendorMemberGroups.stream().map(MemberGroupMapper::map).toList();
    }
}
