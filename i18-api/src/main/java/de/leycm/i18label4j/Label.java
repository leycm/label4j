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
import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.serializer.LabelSerializer;
import de.leycm.i18label4j.exception.SerializationException;
import de.leycm.i18label4j.exception.FormatException;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

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
 * {@link MappingRule} when {@link #resolve()}
 * or its overloads are called. The raw, un-substituted text is returned
 * by {@link #rawOf(Locale)}.</p>
 *
 * <p>Thread Safety: Implementations are not required to be thread-safe.
 * The mutable mapping state added via {@link #map} should only be
 * modified from a single thread unless the implementation documents
 * otherwise.</p>
 *
 * @since 1.0
 * @see LabelProvider
 * @see Mapping
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface Label extends Mappable<Label> {

    // ==== Factory Methods ===================================================

    static <T> @NonNull LabelConstructor<T> constructor(final @NonNull String key, Class<T> type) {
        return new LabelConstructor<>(key, null);
    }

    static <T> @NonNull LabelConstructor<T> constructor(final @NonNull String key, final @Nullable String fallback, Class<T> type) {
        return new LabelConstructor<>(key, fallback);
    }

    /**
     * Creates a translatable label for the given key using the default {@link LabelProvider}.
     *
     * <p>When no translation is found for the current locale, the provider's default
     * fallback strategy is applied (see {@link LabelProvider#createI18Label(String, String)}).</p>
     *
     * @param key the translation key; never {@code null}
     * @return a translatable label; never {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    static @NonNull Label of(final @NonNull String key) {
        return of(LabelProvider.getInstance(), key);
    }

    /**
     * Creates a translatable label for the given key using the default {@link LabelProvider},
     * with a static string used as the fallback when no translation is available.
     *
     * @param key      the translation key; never {@code null}
     * @param fallback the text returned when no translation is found; can be {@code null}
     * @return a translatable label; never {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    static @NonNull Label of(final @NonNull String key,
                             final @Nullable String fallback) {
        return of(LabelProvider.getInstance(), key, fallback);
    }

    /**
     * Creates a translatable label for the given key using the specified {@link LabelProvider}.
     *
     * <p>The provider's default fallback strategy is applied when no translation is found.</p>
     *
     * @param provider the provider responsible for translation lookup; never {@code null}
     * @param key      the translation key; never {@code null}
     * @return a translatable label; never {@code null}
     * @throws NullPointerException if {@code provider} or {@code key} is {@code null}
     */
    static @NonNull Label of(final @NonNull LabelProvider provider,
                             final @NonNull String key) {
        return provider.createI18Label(key, null);
    }

    /**
     * Creates a translatable label for the given key using the specified {@link LabelProvider},
     * with a static string used as the fallback when no translation is available.
     *
     * @param provider the provider responsible for translation lookup; never {@code null}
     * @param key      the translation key; never {@code null}
     * @param fallback the text returned when no translation is found; can be {@code null}
     * @return a translatable label; never {@code null}
     * @throws NullPointerException if {@code provider} or {@code key} is {@code null}
     */
    static @NonNull Label of(final @NonNull LabelProvider provider,
                             final @NonNull String key,
                             final @Nullable String fallback) {
        return provider.createI18Label(key, fallback);
    }

    /**
     * Creates a literal (non-translatable) label with the given static text,
     * using the default {@link LabelProvider}.
     *
     * <p>Literal labels are returned as-is and are never looked up in a resource bundle.
     * They are useful for dynamic or already-localized strings that should still
     * participate in the {@link Label} abstraction (e.g. for consistent component handling).</p>
     *
     * @param literal the static text content; never {@code null}
     * @return a literal label; never {@code null}
     * @throws NullPointerException if {@code literal} is {@code null}
     */
    static @NonNull Label literal(final @NonNull String literal) {
        return literal(LabelProvider.getInstance(), literal);
    }

    /**
     * Creates a literal (non-translatable) label with the given static text,
     * using the specified {@link LabelProvider}.
     *
     * @param provider the provider to associate with this label; never {@code null}
     * @param literal  the static text content; never {@code null}
     * @return a literal label; never {@code null}
     * @throws NullPointerException if {@code provider} or {@code literal} is {@code null}
     */
    static @NonNull Label literal(final @NonNull LabelProvider provider,
                                  final @NonNull String literal) {
        return provider.createLiteralLabel(literal);
    }

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

    // ==== Mapped Resolution =================================================

    /**
     * Resolves and applies all registered {@link Mapping} objects using
     * the provider's default {@link Locale} and default mapping rule.
     *
     * @return the substituted string; never {@code null}
     * @throws IllegalArgumentException if the resolved text exceeds
     *         the mapping engine's input size limit
     */
    default @NonNull String resolve() {
        return resolve(getProvider().getDefaultLocale());
    }

    /**
     * Resolves and applies all registered {@link Mapping} objects using
     * the provider's default {@link Locale}, then formats the result
     * into type {@code T}.
     *
     * @param <T>  the target type
     * @param type the class of the target type; never {@code null}
     * @return the formatted, substituted value; never {@code null}
     * @throws FormatException if no
     *         serializer is registered for {@code type}, or if conversion fails
     * @throws IllegalArgumentException if {@code type} is not supported
     *         or the text exceeds the mapping engine's input size limit
     */
    default <T> @NonNull T resolve(final @NonNull Class<T> type) {
        return resolve(getProvider().getDefaultLocale(), type);
    }

    /**
     * Resolves and applies all registered {@link Mapping} objects for
     * the given {@link Locale}, then formats the result into type {@code T}.
     *
     * @param <T>    the target type
     * @param locale the locale to resolve for; never {@code null}
     * @param type   the class of the target type; never {@code null}
     * @return the formatted, substituted value; never {@code null}
     * @throws FormatException if no
     *         serializer is registered for {@code type}, or if conversion fails
     * @throws IllegalArgumentException if {@code type} is not supported
     *         or the text exceeds the mapping engine's input size limit
     */
    default <T> @NonNull T resolve(final @NonNull Locale locale, final @NonNull Class<T> type) {
        return getProvider().format(resolve(locale), type);
    }

    /**
     * Resolves and applies all registered {@link Mapping} objects for
     * the given {@link Locale} using the provider's default mapping rule.
     *
     * @param locale the locale to resolve for; never {@code null}
     * @return the substituted string; never {@code null}
     * @throws IllegalArgumentException if the resolved text exceeds
     *         the mapping engine's input size limit
     */
    default @NonNull String resolve(@NonNull Locale locale) {
        return getProvider().getDefaultMappingRule().apply(rawOf(locale), getMappings());
    }

    // ==== Localized Resolution ==============================================

    /**
     * Resolves this label using the provider's default {@link Locale},
     * without applying any placeholder substitutions.
     *
     * @return the raw resolved entry; never {@code null}
     */
    default @NonNull Localization localizationOfDefault() {
        return localizationOf(getProvider().getDefaultLocale());
    }


    /**
     * Resolves this label for the given {@link Locale} without applying
     * any placeholder substitutions.
     *
     * <p>For locale-aware labels this performs a translation lookup via
     * the {@link LabelProvider}. For literal labels this simply returns
     * the fixed {@link Localization} regardless of the locale.</p>
     *
     * @param locale the locale to resolve for; never {@code null}
     * @return the raw resolved entry; never {@code null}
     */
    @NonNull Localization localizationOf(@NonNull Locale locale);

    // ==== Raw Resolution ====================================================

    /**
     * Resolves this label using the provider's default {@link Locale},
     * without applying any placeholder substitutions.
     *
     * @return the raw resolved string; never {@code null}
     */
    default @NonNull String rawOfDefault() {
        return rawOf(getProvider().getDefaultLocale());
    }

    /**
     * Resolves this label for the given {@link Locale} without applying
     * any placeholder substitutions.
     *
     * <p>For locale-aware labels this performs a translation lookup via
     * the {@link LabelProvider}. For literal labels this simply returns
     * the fixed literal string regardless of the locale.</p>
     *
     * @param locale the locale to resolve for; never {@code null}
     * @return the raw resolved string; never {@code null}
     */
    @NonNull String rawOf(@NonNull Locale locale);



    // ==== Serialization ====================================================

    /**
     * Serializes this label into the requested type {@code T} using the
     * registered {@link LabelSerializer}.
     *
     * @param <T>  the target type
     * @param type the class of the target type; never {@code null}
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
     * @param obj the object to compare; can be {@code null}
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