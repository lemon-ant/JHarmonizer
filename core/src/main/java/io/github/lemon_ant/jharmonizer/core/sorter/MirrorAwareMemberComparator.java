package io.github.lemon_ant.jharmonizer.core.sorter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Domain model for a class member. Replace with your real implementation.
 */
interface Member {
    /**
     * Stable unique identifier within the compilation unit (e.g., FQN+offset).
     */
    String id();

    /**
     * Simple name for deterministic tie-breaking.
     */
    String simpleName();

    /**
     * Original position (e.g., start offset or ordinal) in the source file.
     */
    int originalPosition();
}

/**
 * Rules that assign a baseline group (bucket) and tell whether a member is dependency-sensitive.
 * Lower group index = earlier in the file.
 */
interface GroupingRules {
    int groupIndex(Member member);

    /**
     * True for members whose placement can be pulled earlier by dependencies (e.g., constants).
     */
    boolean isDependencySensitive(Member member);
}

/**
 * Provides direct dependency edges: A -> {B, C, ...}
 * I.e., A (the key) depends on each member in the returned set.
 */
@SuppressWarnings("PMD")
interface DependencyGraph {
    Set<Member> directDependencies(Member member);
}

/**
 * Intra-group ordering rules.
 * Return a Comparator that defines "who goes first" inside a given group.
 * If a group has no special rules, return null and the comparator will fallback to sameGroupOptions.
 */
@SuppressWarnings("PMD")
interface IntraGroupOrder {
    Comparator<Member> comparatorForGroup(int groupIndex);
}

/**
 * Options for ordering inside the same compiled group (fallback after IntraGroupOrder).
 */
final class OptionsWithinSameGroup {
    /**
     * Keep-by-originalPosition when true.
     */
    final boolean preserveOriginalOrder;

    /**
     * Additionally sort by name if true (after original position if enabled).
     */
    final boolean alsoSortByName;

    OptionsWithinSameGroup(boolean preserveOriginalOrder, boolean alsoSortByName) {
        this.preserveOriginalOrder = preserveOriginalOrder;
        this.alsoSortByName = alsoSortByName;
    }

    static OptionsWithinSameGroup keepOriginalThenByName() {
        return new OptionsWithinSameGroup(true, true);
    }

    static OptionsWithinSameGroup keepOriginalOnly() {
        return new OptionsWithinSameGroup(true, false);
    }

    static OptionsWithinSameGroup byNameOnly() {
        return new OptionsWithinSameGroup(false, true);
    }

    static OptionsWithinSameGroup none() {
        return new OptionsWithinSameGroup(false, false);
    }
}

/**
 * Full analysis result used by the comparator.
 * <p>
 * representativeForCompare:
 * - If dependencies exist: the chosen "root" representative (topmost dependency) of the chain.
 * - If no dependencies: the member itself (self).
 * <p>
 * effectiveGroupIndex:
 * - Minimum among baseline group and ALL transitive dependencies' compiled groups.
 */
record Analysis(int effectiveGroupIndex, Member representativeForCompare, boolean hasAnyDependency) {}

/**
 * Comparator that implements:
 * 1) Reflexivity: compare(x, x) == 0
 * 2) Mirror/symmetric dependency rule:
 * - If RIGHT depends (transitively) on LEFT -> RIGHT is "greater" (goes lower) => return -1
 * - If LEFT  depends (transitively) on RIGHT -> LEFT  is "greater"             => return  1
 * (We run reachability on representatives: root/self.)
 * 3) Compare by compiled group (lower index first).
 * 4) Inside the same group, apply **group-specific rules** (IntraGroupOrder),
 * then fallback to OptionsWithinSameGroup (originalPosition, then name, then id),
 * finally actuals if still equal.
 * <p>
 * Cycles are considered invalid model/code and cause IllegalStateException with a readable path.
 */
@SuppressWarnings("PMD")
public final class MirrorAwareMemberComparator implements Comparator<Member> {

    private final GroupingRules groupingRules;
    private final DependencyGraph dependencyGraph;
    private final IntraGroupOrder intraGroupOrder; // per-group rules
    private final OptionsWithinSameGroup sameGroupOptions;

    // Memoization cache: per-member analysis
    private final Map<String, Analysis> analysisCache = new HashMap<>();

    public MirrorAwareMemberComparator(
            GroupingRules groupingRules,
            DependencyGraph dependencyGraph,
            IntraGroupOrder intraGroupOrder,
            OptionsWithinSameGroup sameGroupOptions) {
        this.groupingRules = Objects.requireNonNull(groupingRules, "groupingRules");
        this.dependencyGraph = Objects.requireNonNull(dependencyGraph, "dependencyGraph");
        this.intraGroupOrder = intraGroupOrder; // may be null
        this.sameGroupOptions =
                (sameGroupOptions != null) ? sameGroupOptions : OptionsWithinSameGroup.keepOriginalThenByName();
    }

    private static <T> Set<T> safeSet(Set<T> maybeNull) {
        return (maybeNull == null) ? Collections.emptySet() : maybeNull;
    }

    @Override
    public int compare(Member left, Member right) {
        // 0) Reflexivity: identical object -> 0
        if (left == right) return 0;

        Analysis leftAnalysis = analyze(left);
        Analysis rightAnalysis = analyze(right);

        Member leftRep = leftAnalysis.representativeForCompare();
        Member rightRep = rightAnalysis.representativeForCompare();

        // 1) Mirror/symmetric dependency rule on representatives (root/self)
        //    RIGHT depends on LEFT -> RIGHT "greater" => LEFT comes first => return -1
        if (isReachable(rightRep, leftRep)) return -1;

        //    LEFT depends on RIGHT -> LEFT "greater" => RIGHT comes first => return  1
        if (isReachable(leftRep, rightRep)) return 1;

        // 2) Effective group comparison (lower goes first)
        int leftGroup = leftAnalysis.effectiveGroupIndex();
        int rightGroup = rightAnalysis.effectiveGroupIndex();
        int groupComparison = Integer.compare(leftGroup, rightGroup);
        if (groupComparison != 0) return groupComparison;

        // 3) Same group tie-breaking — use *group-specific rules first*, on representatives.
        int intra = compareWithinGroup(leftGroup, leftRep, rightRep);
        if (intra != 0) return intra;

        // 4) Final deterministic fallback to keep comparator total (reps)
        int idComparison = leftRep.id().compareTo(rightRep.id());
        if (idComparison != 0) return idComparison;

        // If representatives are indistinguishable, compare actuals to guarantee totality
        int actualPositionComparison = Integer.compare(left.originalPosition(), right.originalPosition());
        if (actualPositionComparison != 0) return actualPositionComparison;

        return left.id().compareTo(right.id());
    }

    // ======================================================================
    // TODO(Anton, 2025-09-27): RETHINK DEP-CHAIN RESOLUTION STRATEGY
    // ----------------------------------------------------------------------
    // Current approach:
    //   - For dependency-sensitive members we:
    //       1) compute each direct dep's Analysis (which already picked its own root and group),
    //       2) compute effectiveGroupIndex(member) = min(baseline, all dep.effectiveGroupIndex),
    //       3) choose the anchor among deps that attain that earliest group by
    //          the *same rules as comparator within that group* (see compareUsingFixedAnalyses),
    //       4) set representativeForCompare = chosen anchor (or self if minimum == baseline).
    //
    // NOTE: We intentionally do NOT call `compare()` while analyzing, to avoid re-entrancy during recursion.
    //       Instead, we use the safe equivalent `compareUsingFixedAnalyses(...)`, which applies the same
    //       ordering logic but reads already-computed Analysis from the cache.
    //
    // Concerns to revisit:
    //   [A] Anchor semantics vs group rules; verify against real-world cases.
    //   [B] Consider pre-pass topo order (Kahn) or SCC compression (Tarjan) if graphs grow larger.
    // ======================================================================

    /**
     * Compare members within the same group:
     * - firstly by group-specific comparator (if provided),
     * - then fallback to sameGroupOptions (originalPosition, name),
     * - return 0 if still equal.
     */
    private int compareWithinGroup(int groupIndex, Member a, Member b) {
        // Group-specific rules have priority
        Comparator<Member> groupCmp = (intraGroupOrder != null) ? intraGroupOrder.comparatorForGroup(groupIndex) : null;

        if (groupCmp != null) {
            int res = groupCmp.compare(a, b);
            if (res != 0) return res;
        }

        // Fallback rules: original position then name (as configured)
        if (sameGroupOptions.preserveOriginalOrder) {
            int positionComparison = Integer.compare(a.originalPosition(), b.originalPosition());
            if (positionComparison != 0) return positionComparison;
        }

        if (sameGroupOptions.alsoSortByName) {
            int nameComparison = a.simpleName().compareTo(b.simpleName());
            return nameComparison;
        }

        return 0;
    }

    /**
     * Analyze a member (with cycle detection) and memoize.
     */
    private Analysis analyze(Member member) {
        Analysis cached = analysisCache.get(member.id());
        if (cached != null) return cached;

        // Keep both a stack (for readable path) and a set (for O(1) membership checks).
        Deque<Member> recursionStack = new ArrayDeque<>();
        Set<String> inStackIds = new HashSet<>();

        Analysis computed = analyzeRecursive(member, recursionStack, inStackIds);
        analysisCache.put(member.id(), computed);
        return computed;
    }

    /**
     * Recursive analysis that:
     * - Detects cycles and throws IllegalStateException with a readable path.
     * - Computes effectiveGroupIndex as MIN(baseline, all transitive deps' compiled groups).
     * - Picks representativeForCompare:
     * * If min == baseline -> representative = SELF.
     * * Else -> among deps that attain the minimum group, pick the **first by the same rules**
     * (using compareUsingFixedAnalyses for that group).
     */
    private Analysis analyzeRecursive(Member member, Deque<Member> recursionStack, Set<String> inStackIds) {
        // ---- Detect cycles and fail hard ----
        if (!inStackIds.add(member.id())) {
            throw new IllegalStateException(buildCycleMessage(member, recursionStack));
        }
        recursionStack.push(member);

        try {
            int baseline = groupingRules.groupIndex(member);

            // Not dependency-sensitive → stay at baseline; representative = self
            if (!groupingRules.isDependencySensitive(member)) {
                return new Analysis(baseline, member, false);
            }

            Set<Member> directDeps = safeSet(dependencyGraph.directDependencies(member));
            if (directDeps.isEmpty()) {
                // Sensitive but no deps → baseline; representative = self
                return new Analysis(baseline, member, false);
            }

            // Recurse into direct dependencies and accumulate the earliest group across the whole chain.
            int earliestGroup = baseline; // start with self in the candidate set
            Member earliestAnchor = null; // anchor representative that “pulls” us earlier

            for (Member dep : directDeps) {
                Analysis depAnalysis = analysisCache.get(dep.id());
                if (depAnalysis == null) {
                    depAnalysis = analyzeRecursive(dep, recursionStack, inStackIds);
                    analysisCache.put(dep.id(), depAnalysis);
                }

                int depGroup = depAnalysis.effectiveGroupIndex();
                Member depRoot = depAnalysis.representativeForCompare();

                if (depGroup < earliestGroup) {
                    earliestGroup = depGroup;
                    earliestAnchor = depRoot; // reset anchor to this dep's root
                } else if (depGroup == earliestGroup) {
                    // Both candidates attain the same earliest group:
                    // choose by EXACTLY the same rules as the main comparator would,
                    // but using fixed analyses (no re-entrancy).
                    earliestAnchor = pickAnchorUsingFixedAnalyses(earliestGroup, earliestAnchor, depRoot);
                }
            }

            // If our own baseline is the global minimum → representative is SELF (keep jharmonizer group).
            if (earliestGroup == baseline) {
                return new Analysis(baseline, member, true);
            }

            // Otherwise we are lifted to the earlier group; representative is the chosen anchor.
            // By construction earliestAnchor != null when earliestGroup < baseline.
            return new Analysis(earliestGroup, earliestAnchor, true);

        } finally {
            recursionStack.pop();
            inStackIds.remove(member.id());
        }
    }

    /**
     * Pick anchor among candidates that share the same earliest group,
     * using the *same* ordering as the main comparator, but reading Analysis from cache.
     */
    private Member pickAnchorUsingFixedAnalyses(int groupIndex, Member a, Member b) {
        if (a == null) return b;
        if (b == null) return a;
        int cmp = compareUsingFixedAnalyses(groupIndex, a, b);
        return (cmp <= 0) ? a : b; // stable
    }

    /**
     * Compare two members as if the main comparator compared them *knowing* they are in the same group.
     * Uses already-computed Analysis from analysisCache; never calls analyze() (no re-entrancy).
     * <p>
     * Order:
     * 1) Mirror dependency rule on representatives (root/self), using the cache.
     * 2) Intra-group rules (IntraGroupOrder).
     * 3) Fallback: originalPosition, then name, then id.
     */
    private int compareUsingFixedAnalyses(int groupIndex, Member a, Member b) {
        Analysis aA = analysisCache.get(a.id());
        Analysis bA = analysisCache.get(b.id());
        if (aA == null || bA == null) {
            // Should not happen: analyses for deps are computed before anchor selection
            // but guard defensively to avoid NPE and keep deterministic behavior.
            return fallbackByOriginalThenNameThenId(a, b);
        }

        Member aRep = aA.representativeForCompare();
        Member bRep = bA.representativeForCompare();

        // 1) Mirror dependency rule on representatives
        if (isReachable(bRep, aRep)) return -1; // b depends on a -> a comes first
        if (isReachable(aRep, bRep)) return 1; // a depends on b -> b comes first

        // 2) Intra-group order for this group
        int intra = compareWithinGroup(groupIndex, aRep, bRep);
        if (intra != 0) return intra;

        // 3) Deterministic fallback on representatives
        int idCmp = aRep.id().compareTo(bRep.id());
        if (idCmp != 0) return idCmp;

        // And final-final fallback on actuals (should be rare)
        return fallbackByOriginalThenNameThenId(a, b);
    }

    private int fallbackByOriginalThenNameThenId(Member a, Member b) {
        int pos = Integer.compare(a.originalPosition(), b.originalPosition());
        if (pos != 0) return pos;
        int name = a.simpleName().compareTo(b.simpleName());
        if (name != 0) return name;
        return a.id().compareTo(b.id());
    }

    /**
     * Human-readable cycle message like: A[idA] -> B[idB] -> C[idC] -> A[idA]
     */
    private String buildCycleMessage(Member current, Deque<Member> stack) {
        List<Member> path = new ArrayList<>(stack);
        Collections.reverse(path); // bottom -> top
        StringBuilder message = new StringBuilder("Detected dependency cycle among members: ");
        for (int i = 0; i < path.size(); i++) {
            Member m = path.get(i);
            message.append(m.simpleName()).append("[").append(m.id()).append("]");
            message.append(" -> ");
        }
        message.append(current.simpleName()).append("[").append(current.id()).append("]");
        return message.toString();
    }

    // ----------------------------------------------------------------------
    // TODO(Anton, 2025-09-27): RE-EVALUATE REACHABILITY HEURISTIC
    // - Confirm we should test reachability on representatives (root/self), not raw nodes.
    // - Consider caching reachability or precomputing transitive reduction if graphs get large.
    // - If SCCs appear (shouldn't in valid code), ensure IllegalStateException is thrown earlier.
    // ----------------------------------------------------------------------
    private boolean isReachable(Member from, Member to) {
        if (from.id().equals(to.id())) return true; // same node counts as reachable (root/self)
        Deque<Member> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        stack.push(from);
        while (!stack.isEmpty()) {
            Member current = stack.pop();
            if (!visited.add(current.id())) continue;
            for (Member next : safeSet(dependencyGraph.directDependencies(current))) {
                if (next.id().equals(to.id())) return true;
                stack.push(next);
            }
        }
        return false;
    }
}
