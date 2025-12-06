package io.github.lemon_ant.jharmonizer.core.sorter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import spoon.reflect.declaration.CtTypeMember;

@EqualsAndHashCode
@AllArgsConstructor
@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
// TODO Complete it
final class CtTypeMemberComparator implements Comparator<CtTypeMember> {

    private final List<CompiledMemberGroup> configArrangementRules;

    @Override
    public int compare(CtTypeMember member1, CtTypeMember member2) {
        if (member1.getPosition().isValidPosition() && member2.getPosition().isValidPosition()) {
            // TODO Sort alphabetically
            return Integer.compare(
                    member1.getPosition().getSourceStart(),
                    member2.getPosition().getSourceStart());
        }
        return 0;
    }
}
