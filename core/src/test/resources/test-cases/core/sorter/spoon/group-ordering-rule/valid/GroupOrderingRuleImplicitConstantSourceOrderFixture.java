// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.group_ordering_rule;

import java.time.format.DateTimeFormatter;

public class GroupOrderingRuleImplicitConstantSourceOrderFixture {

    private static final String zPattern = "yyyy/MM/dd HH:mm:ss";
    private static final DateTimeFormatter aFormatter = DateTimeFormatter.ofPattern(zPattern);
    private static final int cAnchor = Integer.parseInt("1");
}
