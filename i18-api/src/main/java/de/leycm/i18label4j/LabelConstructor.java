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
package de.leycm.i18label4j;

import de.leycm.i18label4j.mapping.Mappable;
import de.leycm.i18label4j.mapping.Mapping;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Builder class for constructing {@link Label} instances with dynamic
 * mappings based on a provided object.
 *
 * <p>A {@link LabelConstructor} allows you to define a label key and an
 * optional fallback string, along with a set of dynamic mappings that can
 * be resolved using a provided object. When the {@link #create(Object)}
 * method is called, a new {@link Label} instance is created with all
 * registered mappings applied, and the object resolvers are triggered to
 * generate their respective mapping values.</p>
 *
 * <p>Thread Safety: This class is not thread-safe and should not be shared
 * across threads. Each thread should create its own instance when needed.</p>
 *
 * @param <O> the type of the object used for resolving dynamic mappings
 *
 * @since 1.1
 * @see Label
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
// note: this is not thread-safe by design, because it is not intended to be shared across threads either
public final class LabelConstructor<O> implements Mappable<LabelConstructor<O>> {
    private final String key;
    private final @Nullable String fallback;

    private final Map<String, Function<O, Object>> objectResolvers = new ConcurrentHashMap<>();
    private final Set<Mapping> mappings = new HashSet<>();

    /**
     * Constructs a new {@link LabelConstructor} with the specified key and
     * optional fallback value.
     *
     * @param key      the label key for translation lookup; never {@code null}
     * @param fallback the optional fallback string used when no translation
     *                 is found; may be {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    // note: use Label.constructor(String, String) method to create instances of this class
    LabelConstructor(final @NonNull String key, final @Nullable String fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull LabelConstructor<O> map(final @NonNull Mapping mapping) throws IllegalArgumentException {
        if (mappings.contains(mapping))
            throw new IllegalArgumentException(
                    "Mapping with key \"" + mapping.key() + "\" already exists for this LabelConstructor.");
        if (objectResolvers.containsKey(mapping.key()))
            throw new IllegalArgumentException(
                    "Mapping with key \"" + mapping.key() + "\" already exists as  for this LabelConstructor.");
         mappings.add(mapping);
         return this;
    }

    /**
     * Registers a dynamic placeholder mapping on this instance.
     *
     * <p>Delegates to {@link #map(Mapping)} by creating a new
     * {@link Mapping} from the provided key and supplier.</p>
     *
     * @param key      the placeholder key; never {@code null}
     * @param function the function evaluated at mapping time;
     *                 never {@code null}
     * @return this instance for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     * @throws NullPointerException     if {@code key} or {@code function}
     *                                  is {@code null}
     */
    public @NonNull LabelConstructor<O> map(final @NonNull String key,
                                            final @NonNull Function<O, Object> function) throws IllegalArgumentException {
        // note: using a dummy mapping to check for existing keys in the mappings set, since it is keyed by the mapping's key
        if (mappings.contains(new Mapping(key, () -> null)))
            throw new IllegalArgumentException(
                    "Mapping with key \"" + key + "\" already exists for this LabelConstructor.");
        if (objectResolvers.containsKey(key))
            throw new IllegalArgumentException(
                    "Mapping with key \"" + key + "\" already exists for this LabelConstructor.");
        objectResolvers.put(key, function);
        return this;
    }

    /**
     * Creates a new {@link Label} instance based on the current state of this
     * constructor and the provided object.
     *
     * <p>All registered mappings are applied to the new label, and all
     * object resolvers are triggered with the provided object to generate
     * their respective mapping values.</p>
     *
     * @param o the object used for resolving dynamic mappings; never {@code null}
     * @return a new {@link Label} instance with all mappings applied;
     *         never {@code null}
     */
    public @NonNull Label create(O o) {
        final Label result = Label.of(key, fallback);

        for (final Mapping mapping : mappings) {
            result.map(mapping);
        }

        for (final Map.Entry<String, Function<O, Object>> entry : objectResolvers.entrySet()) {
            // note: supplier triggering a function with the provided object as argument
            result.map(entry.getKey(), () -> entry.getValue().apply(o));
        }

        return result;
    }

}
