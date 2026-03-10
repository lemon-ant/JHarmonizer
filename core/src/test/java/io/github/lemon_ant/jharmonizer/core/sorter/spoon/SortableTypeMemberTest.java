package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class SortableTypeMemberTest {

    @Test
    void constructor_selfRepresentative_usesSelfAsRepresentativeSortableTypeMember() {
        CtTypeMember valueFieldMember = requireFixtureMemberBySimpleName("VALUE");
        Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider =
                SortableTypeMember.getOrderingKeyProvider();

        SortableTypeMember sortableTypeMember = new SortableTypeMember(valueFieldMember, Set.of(), orderingKeyProvider);

        assertThat(sortableTypeMember.getRepresentativeSortableTypeMember()).isSameAs(sortableTypeMember);
    }

    @Test
    void constructor_explicitRepresentative_reusesProvidedSortableTypeMember() {
        CtTypeMember valueFieldMember = requireFixtureMemberBySimpleName("VALUE");
        CtTypeMember readFieldMember = requireFixtureMemberBySimpleName("READ");
        Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider =
                SortableTypeMember.getOrderingKeyProvider();

        SortableTypeMember representativeSortableTypeMember =
                new SortableTypeMember(valueFieldMember, Set.of(), orderingKeyProvider);
        SortableTypeMember dependentSortableTypeMember = new SortableTypeMember(
                readFieldMember, representativeSortableTypeMember, Set.of(valueFieldMember), orderingKeyProvider);

        assertThat(dependentSortableTypeMember.getRepresentativeSortableTypeMember())
                .isSameAs(representativeSortableTypeMember);
        assertThat(representativeSortableTypeMember.getOrderingKey())
                .isSameAs(orderingKeyProvider.apply(valueFieldMember));
    }

    private static CtTypeMember requireFixtureMemberBySimpleName(String expectedSimpleName) {
        return SpoonTestCaseUtils.requireTypeMemberBySimpleName(Constants.FIXTURE_MEMBERS, expectedSimpleName);
    }

    private static final class Constants {

        private static final String FIXTURE_CLASSPATH_RESOURCE =
                "/test-cases/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleFieldInitializerTieFixture.java";
        private static final URL FIXTURE_RESOURCE_URL =
                SortableTypeMemberTest.class.getResource(FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> FIXTURE_MEMBERS =
                streamExplicitSourceTypeMembers(FIXTURE_MAIN_TYPE).toList();

        private Constants() {}
    }
}
