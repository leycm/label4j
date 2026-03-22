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

import de.leycm.i18label4j.mapping.Mapping;
import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.serialize.LabelSerializer;
import de.leycm.i18label4j.exception.SerializationException;
import de.leycm.i18label4j.exception.FormatException;

import lombok.NonNull;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Represents a localizable or literal text label.
 *
 * <p>A {@link Label} encapsulates a piece of text that may be resolved
 * into a {@link String} for a given {@link Locale}. Implementations
 * include locale-aware i18n labels backed by a translation source as
 * well as plain literal labels whose value never changes.</p>
 *
 * <p>Labels support placeholder substitution via {@link Mapping} objects.
 * Mappings are applied by the {@link LabelProvider}'s default
 * {@link MappingRule} when {@link #mapped()}
 * or its overloads are called. The raw, un-substituted text is returned
 * by {@link #in(Locale)}.</p>
 *
 * <p>Thread Safety: Implementations are not required to be thread-safe.
 * The mutable mapping state added via {@link #mapTo} should only be
 * modified from a single thread unless the implementation documents
 * otherwise.</p>
 *
 * @since 1.0
 * @see LabelProvider
 * @see Mapping
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface Label {

    // ==== Basic Accessors ===================================================

    /**
     * Returns the {@link LabelProvider} that owns this label.
     *
     * @return the owning provider; never {@code null}
     */
    @NonNull LabelProvider getProvider();

    /**
     * Returns an unmodifiable view of all {@link Mapping} objects
     * currently registered on this label.
     *
     * @return the set of mappings; never {@code null}, may be empty
     */
    @NonNull Set<Mapping> getMappings();

    // ==== Mapping Registration ===============================================

    /**
     * Registers a static placeholder mapping on this label.
     *
     * <p>Delegates to {@link #mapTo(String, Supplier)} by wrapping
     * {@code value} in a constant {@link Supplier}. The key must not
     * already be registered on this label.</p>
     *
     * @param key   the placeholder key; must not be {@code null}
     * @param value the static replacement value; must not be {@code null}
     * @return this label for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     */
    default @NonNull Label mapTo(final @NonNull String key,
                                 final @NonNull Object value) throws IllegalArgumentException {
        return mapTo(key, () -> value);
    }

    /**
     * Registers a dynamic placeholder mapping on this label.
     *
     * <p>Delegates to {@link #mapTo(Mapping)} by creating a new
     * {@link Mapping} from the provided key and supplier. The supplier
     * is evaluated lazily each time {@link #mapped()} is called.</p>
     *
     * @param key      the placeholder key; must not be {@code null}
     * @param supplier the value supplier evaluated at mapping time;
     *                 must not be {@code null}
     * @return this label for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     */
    default @NonNull Label mapTo(final @NonNull String key,
                                 final @NonNull Supplier<Object> supplier) throws IllegalArgumentException {
        return mapTo(new Mapping(key, supplier));
    }

    /**
     * Registers a {@link Mapping} directly on this label.
     *
     * <p>The mapping's key must be unique within this label instance.
     * Duplicate keys are rejected to prevent ambiguous substitution
     * results.</p>
     *
     * @param mapping the mapping to register; must not be {@code null}
     * @return this label for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     */
    @NonNull Label mapTo(final @NonNull Mapping mapping) throws IllegalArgumentException;

    // ==== Raw Resolution ====================================================

    /**
     * Resolves this label using the provider's default {@link Locale},
     * without applying any placeholder substitutions.
     *
     * @return the raw resolved string; never {@code null}
     */
    default @NonNull String in() {
        return in(getProvider().getDefaultLocale());
    }

    /**
     * Resolves this label using the provider's default {@link Locale}
     * and formats the result into the requested type {@code T}.
     *
     * <p>Delegates to {@link LabelProvider#format(String, Class)} after
     * resolving the text via {@link #in()}.</p>
     *
     * @param <T>  the target type
     * @param type the class of the target type; must not be {@code null}
     * @return the formatted value; never {@code null}
     * @throws FormatException if no
     *         serializer is registered for {@code type}, or if conversion fails
     * @throws IllegalArgumentException if {@code type} is not a supported
     *         serialization target
     */
    default <T> @NonNull T in(final @NonNull Class<T> type) {
        return in(getProvider().getDefaultLocale(), type);
    }

    /**
     * Resolves this label for the given {@link Locale} and formats
     * the result into the requested type {@code T}.
     *
     * <p>Delegates to {@link LabelProvider#format(String, Class)} after
     * resolving the text via {@link #in(Locale)}.</p>
     *
     * @param <T>    the target type
     * @param locale the locale to resolve for; must not be {@code null}
     * @param type   the class of the target type; must not be {@code null}
     * @return the formatted value; never {@code null}
     * @throws FormatException if no
     *         serializer is registered for {@code type}, or if conversion fails
     * @throws IllegalArgumentException if {@code type} is not a supported
     *         serialization target
     */
    default <T> @NonNull T in(@NonNull Locale locale, final @NonNull Class<T> type) {
        return getProvider().format(in(locale), type);
    }

    /**
     * Resolves this label for the given {@link Locale} without applying
     * any placeholder substitutions.
     *
     * <p>For locale-aware labels this performs a translation lookup via
     * the {@link LabelProvider}. For literal labels this simply returns
     * the fixed literal string regardless of the locale.</p>
     *
     * @param locale the locale to resolve for; must not be {@code null}
     * @return the raw resolved string; never {@code null}
     */
    @NonNull String in(@NonNull Locale locale);

    /**
     * Resolves and applies all registered {@link Mapping} objects using
     * the provider's default {@link Locale} and default mapping rule.
     *
     * @return the substituted string; never {@code null}
     * @throws IllegalArgumentException if the resolved text exceeds
     *         the mapping engine's input size limit
     */
    default @NonNull String mapped() {
        return mapped(getProvider().getDefaultLocale());
    }

    /**
     * Resolves and applies all registered {@link Mapping} objects using
     * the provider's default {@link Locale}, then formats the result
     * into type {@code T}.
     *
     * @param <T>  the target type
     * @param type the class of the target type; must not be {@code null}
     * @return the formatted, substituted value; never {@code null}
     * @throws FormatException if no
     *         serializer is registered for {@code type}, or if conversion fails
     * @throws IllegalArgumentException if {@code type} is not supported
     *         or the text exceeds the mapping engine's input size limit
     */
    default <T> @NonNull T mapped(final @NonNull Class<T> type) {
        return mapped(getProvider().getDefaultLocale(), type);
    }

    /**
     * Resolves and applies all registered {@link Mapping} objects for
     * the given {@link Locale}, then formats the result into type {@code T}.
     *
     * @param <T>    the target type
     * @param locale the locale to resolve for; must not be {@code null}
     * @param type   the class of the target type; must not be {@code null}
     * @return the formatted, substituted value; never {@code null}
     * @throws FormatException if no
     *         serializer is registered for {@code type}, or if conversion fails
     * @throws IllegalArgumentException if {@code type} is not supported
     *         or the text exceeds the mapping engine's input size limit
     */
    default <T> @NonNull T mapped(final @NonNull Locale locale, final @NonNull Class<T> type) {
        return getProvider().format(mapped(locale), type);
    }

    /**
     * Resolves and applies all registered {@link Mapping} objects for
     * the given {@link Locale} using the provider's default mapping rule.
     *
     * @param locale the locale to resolve for; must not be {@code null}
     * @return the substituted string; never {@code null}
     * @throws IllegalArgumentException if the resolved text exceeds
     *         the mapping engine's input size limit
     */
    default @NonNull String mapped(@NonNull Locale locale) {
        return getProvider().getDefaultMappingRule().apply(in(locale), getMappings());
    }

    // ==== Serialization ====================================================

    /**
     * Serializes this label into the requested type {@code T} using the
     * registered {@link LabelSerializer}.
     *
     * @param <T>  the target type
     * @param type the class of the target type; must not be {@code null}
     * @return the serialized representation; never {@code null}
     * @throws SerializationException if serialization fails
     * @throws IllegalArgumentException if no serializer is registered
     *                                 for {@code type}
     */
    default <T> @NonNull T serialize(Class<T> type) {
        return getProvider().serialize(this, type);
    }

    // ==== Object Methods ===================================================

    /**
     * Returns a string representation of this label.
     *
     * <p>Implementations typically attempt to serialize the label to
     * a {@link String} via the registered serializer, falling back to
     * a synthetic class-identity string if serialization is unavailable.</p>
     *
     * @return a non-{@code null} string representation
     */
    @Override
    @NonNull String toString();

    /**
     * Determines equality between this label and another object.
     *
     * <p>Two labels are considered equal if they resolve to the same
     * logical content. Implementations must define what "same logical
     * content" means, for example, same provider and same key for
     * locale labels, or same provider and same literal for literal labels.</p>
     *
     * @param obj the object to compare; may be {@code null}
     * @return {@code true} if the objects are logically equal
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    int hashCode();
}