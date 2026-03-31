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
import de.leycm.i18label4j.exception.IncompatibleMatchException;
import de.leycm.i18label4j.exception.SerializationException;
import de.leycm.i18label4j.label.LiteralLabel;
import de.leycm.i18label4j.label.LocaleLabel;
import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.serializer.LabelSerializer;
import de.leycm.i18label4j.source.FileSource;
import de.leycm.i18label4j.source.LocalizationSource;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Standard implementation of {@link LabelProvider}.
 *
 * <p>{@link CommonLabelProvider} manages a two-level translation cache
 * keyed first by locale language tag and then by translation key. Cache
 * entries are loaded lazily on the first request for each locale and
 * retained until explicitly cleared. The implementation is constructed
 * via the {@link CommonLabelProviderBuilder} fluent API and must not be re-configured after
 * construction.</p>
 *
 * <p>Serializers are registered per target class in the builder and
 * stored in an internal {@link ConcurrentHashMap}. Calls to
 * {@link #serialize(Label, Class)}, {@link #deserialize(Object)}, and
 * {@link #format(String, Class)} look up the appropriate serializer and
 * delegate to it.</p>
 *
 * <p>Thread Safety: This class is thread-safe. The translation cache and
 * serializer registry both use {@link ConcurrentHashMap}, and all
 * internal state is either immutable or protected by concurrent
 * structures.</p>
 *
 * @since 1.0
 * @see LabelProvider
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class CommonLabelProvider implements LabelProvider {

    // ==== Builder ===========================================================

    /**
     * Creates and returns a new {@link CommonLabelProviderBuilder} instance.
     *
     * @return a new builder; never {@code null}
     */
    @Contract(value = " -> new", pure = true)
    public static @NonNull CommonLabelProviderBuilder builder() {
        return new CommonLabelProviderBuilder();
    }

    /**
     * Convenience method to create a {@link CommonLabelProvider} with a
     * {@link FileSource} based on the given URI and default settings.
     *
     * <p>This is equivalent to calling
     * {@code CommonLabelProvider.builder().build(FileSource.json(sourceUri))}.</p>
     *
     * @param sourceUri the URI of the parent folder of the
     *                  JSON files containing localization data;
     *                  never {@code null}
     * @return a new {@link CommonLabelProvider} using the specified source;
     *         never {@code null}
     * @throws NullPointerException     if {@code sourceUri} is {@code null}
     * @throws IllegalArgumentException if the source cannot be initialized with the given URI
     */
    public static @NonNull LabelProvider create(URI sourceUri) {
        final Locale locale = Locale.getDefault();
        final FileSource source = FileSource.json(sourceUri);
        return CommonLabelProvider.builder()
                .defaultMappingRule(MappingRule.DOLLAR_CURLY)
                .buildWarm(source, locale);
    }

    // ==== Instance State ===================================================

    // locale language-tag -> (translation key -> LocalizedResult)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Localization>>
            translationCache = new ConcurrentHashMap<>();
    // target class -> serializer
    private final ConcurrentHashMap<Class<?>, LabelSerializer<?>>
            serializerRegistry = new ConcurrentHashMap<>();

    private final @NonNull LocalizationSource localizationSource;
    private final @NonNull MappingRule defaultMappingRule;
    private final @NonNull Locale defaultLocale;

    /**
     * Constructs a new {@link CommonLabelProvider} with all required
     * dependencies.
     *
     * <p>Prefer the {@link #builder()} factory over this constructor
     * for a more readable construction experience.</p>
     *
     * @param serializers      serializers to register, keyed by target class;
     *                         never {@code null}
     * @param defaultMappingRule the mapping rule used when none is specified;
     *                           never {@code null}
     * @param source           the localization source for raw translation data;
     *                         never {@code null}
     * @param defaultLocale    the locale used when none is specified;
     *                         never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    public CommonLabelProvider(
            final @NonNull Map<Class<?>, LabelSerializer<?>> serializers,
            final @NonNull MappingRule defaultMappingRule,
            final @NonNull LocalizationSource source,
            final @NonNull Locale defaultLocale) {
        this.serializerRegistry.putAll(serializers);
        this.localizationSource = source;
        this.defaultMappingRule = defaultMappingRule;
        this.defaultLocale = defaultLocale;
    }

    // ==== Configuration ====================================================

    /**
     * Returns the {@link LocalizationSource} used by this provider.
     *
     * @return the localization source; never {@code null}
     */
    @Override
    public @NonNull LocalizationSource getLocalizationSource() {
        return localizationSource;
    }

    /**
     * Returns the default {@link Locale} configured for this provider.
     *
     * @return the default locale; never {@code null}
     */
    @Override
    public @NonNull Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Returns the default {@link MappingRule} configured for this provider.
     *
     * @return the default mapping rule; never {@code null}
     */
    @Override
    public @NonNull MappingRule getDefaultMappingRule() {
        return defaultMappingRule;
    }

    /**
     * {@inheritDoc}
     *
     * @param key      the translation key; never {@code null}
     * @param fallback the value supplier for missing translations;
     *                 never {@code null}
     * @return a new locale-aware label; never {@code null}
     */
    @Override
    public @NonNull Label createI18Label(
            final @NonNull String key,
            final @Nullable String fallback) {
        return new LocaleLabel(this, key, fallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param literal the fixed text value; never {@code null}
     * @return a new literal label; never {@code null}
     */
    @Override
    public @NonNull Label createLiteralLabel(final @NonNull String literal) {
        return new LiteralLabel(this, literal);
    }

    // ==== Translation ======================================================

    /**
     * Performs the internal translation lookup, returning a
     * {@link Localization} that wraps either the found translation
     * or {@code null} when none exists.
     *
     * <p>The locale's translation map is loaded via
     * {@link #loadLocaleMap(Locale)} on the first call and then cached.
     * A cache miss for a specific key within a loaded locale will attempt
     * the default locale as a fallback (unless the requested locale already
     * is the default locale, in which case a {@code null}-wrapped result
     * is returned immediately).</p>
     *
     * @param locale the target locale; never {@code null}
     * @param key    the translation key; never {@code null}
     * @return a {@link Localization} wrapping the translation or
     *         {@code null}; never {@code null}
     * @throws NullPointerException     if any parameter is {@code null}
     * @throws IllegalArgumentException if the locale's translation data
     *                                  cannot be loaded from the source
     */
    @ApiStatus.Internal
    public @NonNull Localization translate(final @NonNull Locale locale,
                                           final @NonNull String key)
            throws IllegalArgumentException {

        final Map<String, Localization> localeMap = loadLocaleMap(locale);

        final Localization existing = localeMap.get(key);
        if (existing != null) return existing;

        final Localization fallback = getDefaultLocale().equals(locale)
                ? new Localization(locale, null)
                : translate(getDefaultLocale(), key);

        // note: race window between the get() above and computeIfAbsent() is harmless
        return localeMap.computeIfAbsent(key, k -> fallback);
    }

    /**
     * Loads and caches the complete translation map for the given locale.
     *
     * <p>On the first call for a locale the data is fetched from the
     * {@link LocalizationSource} and stored in {@link #translationCache}.
     * On subsequent calls the cached map is returned immediately. If the
     * source throws an exception an empty map is cached so that repeated
     * failures do not continuously retry; the original exception is wrapped
     * in an {@link IllegalArgumentException} and rethrown. The cache must
     * be cleared via {@link #clearCache(Locale)} before a retry is
     * possible.</p>
     *
     * @param locale the locale whose map should be loaded;
     *               never {@code null}
     * @return the (possibly empty) translation map; never {@code null}
     * @throws NullPointerException     if {@code locale} is {@code null}
     * @throws IllegalArgumentException if the source fails to load the
     *                                  locale's data — contains the
     *                                  original cause as a chained exception
     */
    @ApiStatus.Internal
    public @NonNull ConcurrentMap<String, Localization> loadLocaleMap(final @NonNull Locale locale)
            throws IllegalArgumentException {

        AtomicReference<IllegalArgumentException> loadException = new AtomicReference<>();

        final ConcurrentMap<String, Localization> localMap = translationCache.computeIfAbsent(locale.toLanguageTag(), tag -> {
            try {
                return localizationSource.getLocalization(locale)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toConcurrentMap(Map.Entry::getKey,
                                e -> new Localization(locale, e.getValue()),
                                (a, b) -> a,
                                ConcurrentHashMap::new));

            } catch (Exception e) {
                loadException.set(new IllegalArgumentException(
                        "Failed to load translations for locale \"" + locale.toLanguageTag()
                                + "\"; an empty map is cached due to the failure, "
                                + "translation attempts will not re-attempt loading "
                                + "until the cache is cleared", e));

                return new ConcurrentHashMap<>();
            }
        });

        IllegalArgumentException err = loadException.get();
        if (err != null) throw err;

        return localMap;
    }

    // ==== Serialization ====================================================

    /**
     * {@inheritDoc}
     *
     * @param <T>   the target type
     * @param label the label to serialize; never {@code null}
     * @param type  the class of the target type; never {@code null}
     * @return the serialized value; never {@code null}
     * @throws SerializationException   if the serializer fails
     * @throws IllegalArgumentException if no serializer is registered
     *                                  for {@code type}
     * @throws NullPointerException     if any parameter is {@code null}
     */
    @Override
    public @NonNull <T> T serialize(final @NonNull Label label,
                                    final @NonNull Class<T> type)
            throws SerializationException, IllegalArgumentException, NullPointerException {
        LabelSerializer<?> serializer = serializerRegistry.get(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        Object result = serializer.serialize(label);
        return type.cast(result);
    }

    /**
     * {@inheritDoc}
     *
     * @param <T>        the source type
     * @param serialized the serialized representation; never {@code null}
     * @return the reconstructed label; never {@code null}
     * @throws DeserializationException if the serializer fails
     * @throws IllegalArgumentException if no serializer is registered for
     *                                  the runtime type of {@code serialized}
     * @throws NullPointerException     if {@code serialized} is {@code null}
     */
    @Override
    public @NonNull <T> Label deserialize(final @NonNull T serialized)
            throws DeserializationException, IllegalArgumentException, NullPointerException {
        Class<?> type = serialized.getClass();
        LabelSerializer<T> serializer = getSafeSerializer(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        return serializer.deserialize(serialized, this);
    }

    /**
     * {@inheritDoc}
     *
     * @param <T>   the target type
     * @param input the raw string to format; never {@code null}
     * @param type  the class of the target type; never {@code null}
     * @return the formatted value; never {@code null}
     * @throws FormatException          if the formatter fails
     * @throws IllegalArgumentException if no serializer is registered
     *                                  for {@code type}
     * @throws NullPointerException     if any parameter is {@code null}
     */
    @Override
    public @NonNull <T> T format(final @NonNull String input,
                                 final @NonNull Class<T> type)
            throws FormatException, IllegalArgumentException, NullPointerException {
        LabelSerializer<?> serializer = serializerRegistry.get(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        Object result = serializer.format(input);
        return type.cast(result);
    }

    /**
     * Retrieves and casts the registered serializer for the given type.
     *
     * <p>The cast is unchecked by nature; a {@link ClassCastException}
     * is caught and re-thrown as an {@link IncompatibleMatchException}
     * with a descriptive message to aid diagnostics.</p>
     *
     * @param <T>  the target type
     * @param type the target class whose serializer should be retrieved;
     *             never {@code null}
     * @return the typed serializer, or {@code null} if none is registered
     * @throws IncompatibleMatchException if the registered serializer's
     *                                    generic type is incompatible with
     *                                    {@code T}
     */
    @SuppressWarnings("unchecked") // cause: we catch ClassCastException explicitly
    private <T> LabelSerializer<T> getSafeSerializer(final @NonNull Class<?> type) {
        try {
            return (LabelSerializer<T>) serializerRegistry.get(type);
        } catch (ClassCastException e) {
            throw new IncompatibleMatchException(type, e);
        }
    }

    // ==== Cache Management =================================================

    /**
     * {@inheritDoc}
     *
     * <p>Removes all entries from the internal translation cache. After
     * this call the next lookup for any locale will reload data from
     * the {@link LocalizationSource}.</p>
     */
    @Override
    public void clearCache() {
        translationCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes and clears the translation cache entry for the given
     * locale. The locale's inner map is cleared to release its memory
     * before the entry is removed from the outer cache.</p>
     *
     * @param locale the locale whose cache should be evicted;
     *               never {@code null}
     */
    @Override
    public void clearCache(final @NonNull Locale locale) {
        Map<?, ?> localeCache = translationCache.remove(locale.toLanguageTag());
        if (localeCache != null) { localeCache.clear(); }
    }

    /**
     * {@inheritDoc}
     *
     * @param locale the locales to evict; never {@code null},
     *               individual elements never {@code null}
     */
    @Override
    public void clearCache(final @NonNull Locale @NonNull ... locale) {
        for (Locale loc : locale) clearCache(loc);
    }

}