// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import lombok.Value;
import org.apache.commons.lang3.Validate;

@Value
public class SrcCharacterRange {
    int startInclusive;
    int endExclusive;

    public SrcCharacterRange(int startInclusive, int endExclusive) {
        Validate.isTrue(startInclusive >= 0, "Range start must be non-negative: %s", startInclusive);
        Validate.isTrue(
                endExclusive >= startInclusive, "Range end must be >= start: %s < %s", endExclusive, startInclusive);
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
    }
}
