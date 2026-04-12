package io.github.lemon_ant.jharmonizer.sorting;

import java.util.Comparator;
import lombok.NonNull;
import lombok.Value;

/**
 * A sortable container identified by an {@link OrderingKey}.
 *
 * <p>The key encapsulates both a {@code name} and a {@link Numeration} type
 * ({@code STATIC} or {@code DYNAMIC}).  The default comparator places all
 * {@code STATIC} members first (sorted by name), followed by all {@code DYNAMIC}
 * members (also sorted by name).
 */
@Value
public class SortableTypeMember {

    /**
     * Defines whether a member is static (fixed, known upfront) or dynamic (runtime/generated).
     * The natural order places {@code STATIC} before {@code DYNAMIC}.
     */
    public enum Numeration {
        STATIC,
        DYNAMIC
    }

    /**
     * The composite ordering key used by comparators.
     *
     * <p>Contains the member's human-readable {@code name} and its {@link Numeration} type.
     * Both fields are mandatory; {@code name} must be non-null and non-blank.
     */
    @Value
    public static class OrderingKey {
        @NonNull
        String name;

        @NonNull
        Numeration numeration;

        public OrderingKey(String name, @NonNull Numeration numeration) {
            if (name == null || name.isBlank()) {
                throw new SortingException("Member name must not be blank");
            }
            this.name = name;
            this.numeration = numeration;
        }
    }

    /**
     * Default comparator: {@code STATIC} members first (by name), then {@code DYNAMIC}
     * members (by name).
     */
    public static final Comparator<SortableTypeMember> DEFAULT_ORDER = Comparator.comparing(
                    (SortableTypeMember member) -> member.getOrderingKey().getNumeration())
            .thenComparing(member -> member.getOrderingKey().getName());

    @NonNull
    OrderingKey orderingKey;

    public static SortableTypeMember staticMember(String name) {
        return new SortableTypeMember(new OrderingKey(name, Numeration.STATIC));
    }

    public static SortableTypeMember dynamicMember(String name) {
        return new SortableTypeMember(new OrderingKey(name, Numeration.DYNAMIC));
    }

    /** Returns the name from the ordering key (convenience accessor). */
    public String getName() {
        return orderingKey.getName();
    }
}
