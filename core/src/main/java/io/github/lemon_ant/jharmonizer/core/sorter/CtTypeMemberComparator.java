package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.MemberGroup;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import spoon.reflect.declaration.CtTypeMember;

@EqualsAndHashCode
@AllArgsConstructor
final class CtTypeMemberComparator implements Comparator<CtTypeMember>, Serializable {
    @Serial
    private static final long serialVersionUID = -6080067198735534451L;

    private final List<MemberGroup> configArrangementRules;

    @Override
    public int compare(CtTypeMember member1, CtTypeMember member2) {

        // TODO Sort alphabetically
        return Integer.compare(
                member1.getPosition().getSourceStart(), member2.getPosition().getSourceStart());
    }
}
