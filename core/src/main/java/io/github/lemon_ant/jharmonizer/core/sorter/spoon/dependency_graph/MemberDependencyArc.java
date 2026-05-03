/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A directed arc in the member dependency graph, linking a type member to an adjacent member
 * via a specific edge kind (e.g., declaration dependency or accessor bundle).
 */
@Value
class MemberDependencyArc {

    @NonNull
    CtTypeMember adjacentMember;

    @NonNull
    MemberDependencyEdgeKind edgeKind;
}
