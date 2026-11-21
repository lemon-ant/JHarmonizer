// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
public class MemberRelocationTestCreator {

    @NonNull
    public static MemberRelocation createMemberRelocation(
            @NonNull List<? extends CtTypeMember> relocatedMembers,
            @Nullable CtTypeMember sortedPredecessor,
            @Nullable CtTypeMember sortedSuccessor) {
        return new MemberRelocation(relocatedMembers, sortedPredecessor, sortedSuccessor);
    }
}
