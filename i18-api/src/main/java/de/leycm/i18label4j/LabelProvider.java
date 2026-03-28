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

import de.leycm.i18label4j.exception.DeserializationException;
import de.leycm.i18label4j.exception.FormatException;
import de.leycm.i18label4j.exception.SerializationException;
import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.serializer.Localization;
import de.leycm.i18label4j.serializer.LabelSerializer;
import de.leycm.i18label4j.source.LocalizationSource;
import de.leycm.init4j.instance.Instanceable;

import lombok.NonNull;

import java.util.Locale;
import java.util.function.Function;

/**
 * Central access point for creating and managing {@link Label} instances.
 *
 * <p>A {@link LabelProvider} coordinates translation lookups, label
 * creation, serialization, and placeholder formatting. It combines a
 * {@link LocalizationSource} for raw translation data with a registry
 * of {@link LabelSerializer} instances
 * that convert labels to and from arbitrary target types such as
 * Adventure {@code Component} objects.</p>
 *
 * <p>Implementations are expected to cache translations internally so
 * that repeated calls for the same locale do not trigger expensive IO.
 * The cache can be evicted per-locale or entirely via the
 * {@code clearCache} family of methods.</p>
 *
 * <p>Thread Safety: The contract for thread safety is defined by each
 * concrete implementation. The reference implementation
 * {@code CommonLabelProvider} is thread-safe.</p>
 *
 * @since 1.0
 * @see Label
 * @see LocalizationSource
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface LabelProvider extends Instanceable {
    
    /**
     * Returns the singleton instance of the {@link LabelProvider}.
     *
     * <p>This method relies on the {@link Instanceable#getInstance(Class)}
     * mechanism to retrieve the registered implementation.</p>
     *
     * <p>The provider must be initialized via
     * {@link Instanceable#register(Instanceable, Class)} before first use
     * to ensure proper configuration and resource loading.</p>
     *
     * @return the singleton instance of {@link LabelProvider}; never {@code null}
     * @throws NullPointerException if no implementation has been registered
     *                              via {@link Instanceable#register(Instanceable, Class)}
     */
    static @NonNull LabelProvider getInstance() {
        return Instanceable.getInstance(LabelProvider.class);
    }

    // ==== Warm Up ===========================================================

    /**
     * Pre-loads and caches translations for the given locales.
     *
     * <p>This method forces the provider to load all translation data
     * for each supplied {@link Locale} into the internal cache. Warm-up
     * eliminates first-request latency in latency-sensitive code paths.</p>
     *
     * <p>Note: The sentinel key {@code "__warmup__"} is used internally
     * to trigger cache population. A translation file containing a key
     * with that name will still be cached correctly and is not excluded.</p>
     *
     * @param localizations the locales to warm up; must not be {@code null},
     *                      individual elements must not be {@code null}
     * @throws IllegalArgumentException if a locale's translation data
     *                                  cannot be loaded from the source
     */
    default void warmUp(final @NonNull Locale @NonNull ... localizations) {
    // note: using the key "__warmup__" to cache translations, translation can still be called "__warmup__".
    final String warmupKey = "__warmup__";

    for (Locale locale : localizations) {
        translate(locale, warmupKey, warmupKey);
    }
    }

    // ==== Configuration ====================================================

    /**
     * Returns the {@link LocalizationSource} used to load raw
     * translation data.
     *
     * @return the localization source; never {@code null}
     */
    @NonNull LocalizationSource getLocalizationSource();

    /**
     * Returns the default {@link Locale} used when no locale is
     * specified explicitly.
     *
     * @return the default locale; never {@code null}
     */
    @NonNull Locale getDefaultLocale();

    /**
     * Returns the default {@link MappingRule} used for placeholder
     * substitution when calling {@link Label#mapped()}.
     *
     * @return the default mapping rule; never {@code null}
     */
    @NonNull MappingRule getDefaultMappingRule();

    // ==== Label Creation ===================================================

    /**
     * Creates a new locale-aware {@link Label} backed by a translation key.
     *
     * <p>When resolved via {@link Label#in(Locale)}, the label performs a
     * translation lookup for the given key. If no translation is found for
     * the requested locale or the default locale, the {@code fallback}
     * function is invoked with the requested locale to produce the result.</p>
     *
     * @param key      the translation key; must not be {@code null}
     * @param fallback the function supplying fallback text when no
     *                 translation is found; must not be {@code null}
     * @return a new locale-aware label; never {@code null}
     */
    @NonNull Label createI18Label(@NonNull String key,
                                  @NonNull Function<Locale, String> fallback);

    /**
     * Creates a new literal {@link Label} whose text never changes
     * regardless of locale.
     *
     * @param literal the fixed text value; must not be {@code null}
     * @return a new literal label; never {@code null}
     */
    @NonNull Label createLiteralLabel(@NonNull String literal);

    // ==== Translation ======================================================

    /**
     * Translates a key into the given locale, returning a fallback
     * string when no translation is available.
     *
     * <p>On a cache miss the translation data for the locale is loaded
     * from the {@link LocalizationSource} and cached. Subsequent calls
     * for the same locale return the cached result without touching
     * the source again.</p>
     *
     * <p>If the requested locale has no translation, and it differs from
     * the default locale, the default locale is tried as a second pass
     * before returning {@code fallback}.</p>
     *
     * @param locale   the target locale; must not be {@code null}
     * @param key      the translation key; must not be {@code null}
     * @param fallback the value returned when no translation exists;
     *                 must not be {@code null}
     * @return the translated string or {@code fallback}; never {@code null}
     * @throws NullPointerException     if any parameter is {@code null}
     * @throws IllegalArgumentException if the locale's translation source
     *                                  fails to load
     */
    default @NonNull String translate(final @NonNull Locale locale,
                                      final @NonNull String key,
                                      final @NonNull String fallback) {
            return translate(locale, key).or(fallback);
    }

    /**
     * Performs the internal translation lookup, returning a
     * {@link Localization} that wraps either the found translation.
     *
     * @param locale the target locale; must not be {@code null}
     * @param key    the translation key; must not be {@code null}
     * @return a {@link Localization} wrapping the translation or
     *         {@code null}; never {@code null}
     * @throws NullPointerException     if any parameter is {@code null}
     * @throws IllegalArgumentException if the locale's translation data
     *                                  cannot be loaded from the source
     */
    @NonNull
    Localization translate(@NonNull Locale locale,
                           @NonNull String key);

    // ==== Serialization ====================================================

    /**
     * Serializes a {@link Label} into the requested type {@code T}.
     *
     * <p>Looks up the registered
     * {@link LabelSerializer} for
     * {@code type} and delegates to its
     * {@link LabelSerializer#serialize(Label)}
     * method.</p>
     *
     * @param <T>   the target type
     * @param label the label to serialize; must not be {@code null}
     * @param type  the class representing the target type;
     *              must not be {@code null}
     * @return the serialized value; never {@code null}
     * @throws SerializationException   if the serializer fails
     * @throws IllegalArgumentException if no serializer is registered
     *                                  for {@code type}
     * @throws NullPointerException     if any parameter is {@code null}
     */
    <T> @NonNull T serialize(@NonNull Label label,
                             @NonNull Class<T> type)
            throws SerializationException;

    /**
     * Deserializes a serialized value back into a {@link Label}.
     *
     * <p>Looks up the registered
     * {@link LabelSerializer} for the
     * runtime type of {@code serialized} and delegates to its
     * {@link LabelSerializer#deserialize(Object, LabelProvider)}
     * method.</p>
     *
     * @param <T>        the source type
     * @param serialized the serialized representation; must not be {@code null}
     * @return the reconstructed label; never {@code null}
     * @throws DeserializationException if the serializer fails
     * @throws IllegalArgumentException if no serializer is registered
     *                                  for the runtime type of {@code serialized}
     * @throws NullPointerException     if {@code serialized} is {@code null}
     */
    <T> @NonNull Label deserialize(@NonNull T serialized)
            throws DeserializationException;

    /**
     * Formats a raw string into the requested type {@code T}.
     *
     * <p>Unlike {@link #serialize(Label, Class)}, this method operates
     * on plain strings — for example converting a MiniMessage string to
     * an Adventure {@code Component} — without involving a full
     * {@link Label} instance.</p>
     *
     * @param <T>   the target type
     * @param input the raw string to format; must not be {@code null}
     * @param type  the class representing the target type;
     *              must not be {@code null}
     * @return the formatted value; never {@code null}
     * @throws FormatException          if the formatter fails
     * @throws IllegalArgumentException if no formatter is registered
     *                                  for {@code type}
     * @throws NullPointerException     if any parameter is {@code null}
     */
    <T> @NonNull T format(@NonNull String input,
                          @NonNull Class<T> type)
            throws FormatException;

    // ==== Cache Management =================================================

    /**
     * Clears all cached translations for all locales.
     *
     * <p>After this call the next translation lookup for any locale will
     * reload data from the {@link LocalizationSource}.</p>
     */
    void clearCache();

    /**
     * Clears the cached translations for a single locale.
     *
     * <p>The entry is removed from the cache entirely. The next lookup
     * for this locale will reload data from the {@link LocalizationSource}.</p>
     *
     * @param locale the locale whose cache should be evicted;
     *               must not be {@code null}
     */
    void clearCache(@NonNull Locale locale);

    /**
     * Clears the cached translations for each of the given locales.
     *
     * <p>Equivalent to calling {@link #clearCache(Locale)} for each
     * element of the array.</p>
     *
     * @param locale the locales to evict; must not be {@code null},
     *               individual elements must not be {@code null}
     */
    void clearCache(@NonNull Locale @NonNull ... locale);
}
