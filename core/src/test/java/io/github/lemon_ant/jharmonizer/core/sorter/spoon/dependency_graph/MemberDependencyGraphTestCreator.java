package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
public class MemberDependencyGraphTestCreator {

    @NonNull
    public static MemberDependencyGraph createGraphWithDeclarationDependencyEdges(
            @NonNull List<MemberDependencyEdge> declarationDependencyEdges) {
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        declarationDependencyEdges.forEach(edge -> memberDependencyGraph.addEdge(
                edge.getProviderMember(), edge.getDependentMember(), MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        return memberDependencyGraph;
    }

    @NonNull
    public static MemberDependencyEdge createDeclarationDependencyEdge(
            @NonNull CtTypeMember providerMember, @NonNull CtTypeMember dependentMember) {
        return new MemberDependencyEdge(providerMember, dependentMember);
    }

    public static final class MemberDependencyEdge {

        private final CtTypeMember providerMember;
        private final CtTypeMember dependentMember;

        private MemberDependencyEdge(@NonNull CtTypeMember providerMember, @NonNull CtTypeMember dependentMember) {
            this.providerMember = providerMember;
            this.dependentMember = dependentMember;
        }

        @NonNull
        public CtTypeMember getProviderMember() {
            return providerMember;
        }

        @NonNull
        public CtTypeMember getDependentMember() {
            return dependentMember;
        }
    }
}
