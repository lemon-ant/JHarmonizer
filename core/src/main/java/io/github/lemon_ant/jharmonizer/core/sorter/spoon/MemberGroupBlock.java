// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A single "group block" that will be printed as a contiguous chunk in the type body.
 */
@Value
class MemberGroupBlock {

    @NonNull
    CompiledMemberGroup compiledMemberGroup;

    @NonNull
    List<@NonNull CtTypeMember> typeMembers;
}
