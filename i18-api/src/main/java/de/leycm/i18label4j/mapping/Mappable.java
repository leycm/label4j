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

import java.util.function.Supplier;

/**
 * Interface representing an entity capable of registering placeholder
 * mappings for value resolution.
 *
 * <p>Implementations of this interface can have different internal
 * structures and mapping storage mechanisms, but they all provide a
 * consistent API for adding mappings. The key requirement is that
 * mapping keys must be unique within the context of a single instance
 * to prevent ambiguous substitutions.</p>
 *
 * @param <T> the type of the implementing class for method chaining
 * @since 1.1
 * @see Mapping
 */
public interface Mappable<T> {

    // ==== Mapping Registration ===============================================

    /**
     * Registers a static placeholder mapping on this instance.
     *
     * <p>Delegates to {@link #map(String, Supplier)} by wrapping
     * {@code value} in a constant {@link Supplier}. The key must not
     * already be registered on this instance.</p>
     *
     * @param key   the placeholder key; never {@code null}
     * @param value the static replacement value; never {@code null}
     * @return this instance for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     * @throws NullPointerException     if {@code key} or {@code value}
     *                                  is {@code null}
     */
    default @NonNull T map(final @NonNull String key,
                           final @NonNull Object value) throws IllegalArgumentException {
        return map(key, () -> value);
    }

    /**
     * Registers a dynamic placeholder mapping on this instance.
     *
     * <p>Delegates to {@link #map(Mapping)} by creating a new
     * {@link Mapping} from the provided key and supplier.</p>
     *
     * @param key      the placeholder key; never {@code null}
     * @param supplier the value supplier evaluated at mapping time;
     *                 never {@code null}
     * @return this instance for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     * @throws NullPointerException     if {@code key} or {@code supplier}
     *                                  is {@code null}
     */
    default @NonNull T map(final @NonNull String key,
                           final @NonNull Supplier<Object> supplier) throws IllegalArgumentException {
        return map(new Mapping(key, supplier));
    }

    /**
     * Registers a {@link Mapping} directly on this instance.
     *
     * <p>The mapping's key must be unique within this instance.
     * Duplicate keys are rejected to prevent ambiguous substitution
     * results.</p>
     *
     * @param mapping the mapping to register; never {@code null}
     * @return this instance for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     * @throws NullPointerException     if {@code mapping} is {@code null}
     */
    @NonNull T map(@NonNull Mapping mapping) throws IllegalArgumentException;
}