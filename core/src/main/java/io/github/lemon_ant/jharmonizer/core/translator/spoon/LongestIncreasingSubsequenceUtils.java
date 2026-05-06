// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utility for computing the Longest Increasing Subsequence (LIS) over an integer array.
 *
 * <p>Used by relocation detection to identify the maximal subset of members that already sit in
 * the correct relative order; the complement is the minimal moved set the developer must
 * relocate to recover the sorted order from the original source.
 */
@UtilityClass
public class LongestIncreasingSubsequenceUtils {

    /**
     * Sentinel value marking array positions that should be ignored when computing the LIS.
     * A position holding this value never participates in the resulting subsequence.
     */
    public static final int UNTRACKED = -1;

    /**
     * Computes a boolean mask marking the elements of {@code values} that participate in a
     * strictly increasing Longest Increasing Subsequence, ignoring {@link #UNTRACKED} entries.
     *
     * <p>Uses the classic O(n log n) patience-sort algorithm with a {@code prev} chain to
     * reconstruct one canonical LIS (the variant ending at the latest possible position).
     *
     * @param values input values; positions equal to {@link #UNTRACKED} are skipped
     * @return a mask the same length as {@code values}; {@code true} where a position belongs
     *         to the chosen LIS
     */
    @NonNull
    @SuppressWarnings("PMD.UseVarargs")
    public static boolean[] computeLisMask(int[] values) {
        int n = values.length;
        boolean[] inLis = new boolean[n];
        int[] tails = new int[n];
        int[] tailPos = new int[n];
        int[] prev = new int[n];
        int len = 0;
        for (int i = 0; i < n; i++) {
            if (values[i] == UNTRACKED) {
                continue;
            }
            int slot = lowerBound(tails, len, values[i]);
            tails[slot] = values[i];
            tailPos[slot] = i;
            prev[i] = slot > 0 ? tailPos[slot - 1] : -1;
            if (slot == len) {
                len++;
            }
        }
        int cursor = len > 0 ? tailPos[len - 1] : -1;
        while (cursor != -1) {
            inLis[cursor] = true;
            cursor = prev[cursor];
        }
        return inLis;
    }

    private static int lowerBound(int[] tails, int len, int value) {
        int lo = 0;
        int hi = len;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (tails[mid] < value) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}
