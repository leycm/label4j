/*
 * This file is part of the i18label4j Library.
 *
 * Licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0)
 * You should have received a copy of the license in LICENSE.LGPL
 * If not, see https://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Copyright 2026 (c) leycm <leycm@proton.me>
 * Copyright 2026 (c) maintainers
 */
package de.leycm.i18label4j.mapping;

import lombok.NonNull;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Immutable record pairing a placeholder key with a value supplier.
 *
 * <p>A {@link Mapping} represents one placeholder substitution entry
 * used during the text-replacement step performed by
 * {@link MappingRule#apply(String, Set)}. The key identifies
 * the placeholder token in the source text, while the {@link Supplier}
 * provides the replacement value lazily — it is called each time
 * {@link #valueAsString()} is invoked, allowing dynamic values that may
 * change between calls.</p>
 *
 * <p>Equality and hashing are based solely on the {@code key} field, so
 * a {@link Set} of {@link Mapping} instances cannot contain
 * two mappings with the same key.</p>
 *
 * <p>Thread Safety: This record is immutable with respect to its fields.
 * Thread safety of the {@link #value()} supplier itself depends on the
 * supplier implementation provided by the caller.</p>
 *
 * @param key   the placeholder key matched in source text; never {@code null}
 * @param value the supplier evaluated to produce the replacement string;
 *              never {@code null}
 *
 * @since 1.0
 * @see MappingRule
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public record Mapping(@NonNull String key,
                      @NonNull Supplier<Object> value
) {

    /**
     * Evaluates the value supplier and converts the result to a
     * {@link String} using {@link String#valueOf(Object)}.
     *
     * <p>If the supplier returns {@code null}, {@link String#valueOf(Object)}
     * will produce the literal string {@code "null"}.</p>
     *
     * @return the string representation of the supplied value;
     *         never {@code null}
     */
    public @NonNull String valueAsString() {
        return String.valueOf(value.get());
    }

    /**
     * Returns the placeholder key matched in source text.
     *
     * @return the placeholder key; never {@code null}
     */
    @Override
    public @NonNull String key() {
        return key;
    }

    /**
     * Returns the supplier evaluated to produce the replacement string.
     *
     * @return the value supplier; never {@code null}
     */
    @Override
    public @NonNull Supplier<Object> value() {
        return value;
    }

    /**
     * Returns a human-readable representation of this mapping.
     *
     * <p>The format is {@code Mapping{key="<key>"}}.</p>
     *
     * @return a non-{@code null} debug string
     */
    @Override
    public @NonNull String toString() {
        return getClass().getSimpleName() + "{" +
                "key=\"" + key + "\"}";
    }

    /**
     * Determines equality based solely on the {@code key} field.
     *
     * <p>Two {@link Mapping} instances are considered equal when their
     * keys are equal, regardless of the value supplier. This ensures
     * that a {@link Set} of mappings cannot contain duplicate
     * keys.</p>
     *
     * @param obj the object to compare; may be {@code null}
     * @return {@code true} if {@code obj} is a {@link Mapping} with an
     *         equal key
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Mapping mapping = (Mapping) obj;
        return key.equals(mapping.key);
    }

    /**
     * Returns a hash code derived from the {@code key} field only,
     * consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return key.hashCode();
    }
}