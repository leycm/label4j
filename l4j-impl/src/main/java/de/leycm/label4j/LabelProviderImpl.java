/*
 * This file is part of label4j - https://github.com/leycm/label4j.
 * Copyright (C) 2026 Lennard [leycm] <leycm@proton.me>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.leycm.label4j;

import de.leycm.label4j.exception.DeserializationException;
import de.leycm.label4j.exception.FormatException;
import de.leycm.label4j.exception.IncompatibleMatchException;
import de.leycm.label4j.exception.SerializationException;
import de.leycm.label4j.locale.Locales;
import de.leycm.label4j.localization.Localization;
import de.leycm.label4j.localization.LocalizationSource;
import de.leycm.label4j.placeholder.PlaceholderRule;
import de.leycm.label4j.serializer.LabelDeserializer;
import de.leycm.label4j.serializer.LabelFormatter;
import de.leycm.label4j.serializer.LabelSerializer;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class LabelProviderImpl implements LabelProvider {

    // ==== Builder ===========================================================

    @Contract(value = " -> new", pure = true)
    public static @NonNull LabelProviderImplBuilder builder() {
        return new LabelProviderImplBuilder();
    }

    // todo: add more simple constructors

    // ==== Instance State ===================================================

    // locale language-tag -> (translation key -> LocalizedResult)
    protected final ConcurrentHashMap<String, ConcurrentHashMap<String, Localization>>
            translationCache = new ConcurrentHashMap<>();
    // target class -> serializer
    protected final Map<Class<?>, LabelSerializer<?>>
            serializerRegistry = new ConcurrentHashMap<>();
    // target class -> deserializer
    protected final Map<Class<?>, LabelDeserializer<?>>
            deserializerRegistry = new ConcurrentHashMap<>();
    // target class -> serializer
    protected final Map<Class<?>, LabelFormatter<?>>
            formaterRegistry = new ConcurrentHashMap<>();

    protected final @NonNull LocalizationSource localizationSource;
    protected final @NonNull PlaceholderRule placeholderRule;
    protected final @NonNull Locale defaultLocale;
    protected final @NonNull Consumer<Exception> loadErrorHandler;
    protected final @NonNull Function<Localization, String> fallbackHandler;

    public LabelProviderImpl(
            final @NonNull Map<Class<?>, LabelSerializer<?>> serializers,
            final @NonNull Map<Class<?>, LabelDeserializer<?>> deserializers,
            final @NonNull Map<Class<?>, LabelFormatter<?>> formater,
            final @NonNull PlaceholderRule placeholderRule,
            final @NonNull LocalizationSource source,
            final @NonNull Locale defaultLocale,
            final @NonNull Consumer<Exception> loadErrorHandler,
            final @NonNull Function<Localization, String> fallbackHandler) {
        this.serializerRegistry.putAll(serializers);
        this.deserializerRegistry.putAll(deserializers);
        this.formaterRegistry.putAll(formater);
        this.localizationSource = source;
        this.placeholderRule = placeholderRule;
        this.defaultLocale = defaultLocale;
        this.loadErrorHandler = loadErrorHandler;
        this.fallbackHandler = fallbackHandler;
    }

    // ==== Configuration =====================================================

    @Override
    public @NonNull LocalizationSource getLocalizationSource() {
        return localizationSource;
    }

    @Override
    public @NonNull PlaceholderRule getPlaceholderRule() {
        return placeholderRule;
    }

    @Override
    public @NonNull Locale getDefaultLocale() {
        return defaultLocale;
    }

    // ==== Fallback Handling =================================================

    @Override
    public String resolveLiteral(final @NonNull Localization localization) {
        return localization.orElseGet(() -> fallbackHandler.apply(localization));
    }

    @Override
    public void warmup(@NonNull Locale @NonNull ... locales) {
        for (final Locale locale : locales) {
            localize(locale, Localization.WARMUP_KEY);
        }
    }

    // ==== Localization ======================================================

    @Override
    public @NonNull Localization localize(
            final @NonNull Locale locale,
            final @NonNull String key
    ) {
        final ConcurrentMap<String, Localization> localeMap = loadLocaleMap(locale);

        final Localization existing = localeMap.get(key);
        if (existing != null) return existing;

        if (locale.equals(defaultLocale) || locale.equals(Locale.ROOT)) {
            final Localization empty = Localization.empty(key, locale);
            localeMap.putIfAbsent(key, empty);
            return empty;
        }

        final Localization fallback = Localization.fallback(localize(defaultLocale, key), locale);

        // note: race window between get() above and putIfAbsent() is harmless
        //       at worst a duplicate Localization object is created and discarded.
        return localeMap.computeIfAbsent(key, k -> fallback);
    }

    @ApiStatus.Internal
    public @NonNull ConcurrentMap<String, Localization> loadLocaleMap(
            final @NonNull Locale locale) {

        final String tag = Locales.localeToFilename(locale);

        ConcurrentMap<String, Localization> existing
                = translationCache.get(tag);

        if (existing != null) return existing;


        Map<String, Localization> loaded;
        try {
            loaded = localizationSource.getLocalization(locale);
        } catch (final Exception e) {
            loadErrorHandler.accept(e);
            loaded = new HashMap<>();
        }

        final ConcurrentHashMap<String, Localization> result
                = new ConcurrentHashMap<>(loaded);

        // note: getting the existing value again after loading is necessary to avoid race conditions
        existing = translationCache.putIfAbsent(tag, result);
        return existing != null ? existing : result;
    }

    // ==== Serialization =====================================================

    @Override
    public @NonNull <T> T serialize(
            final @NonNull Label label,
            final @NonNull Class<T> type
    ) throws SerializationException {

        final LabelSerializer<T> serializer = getConverterSafe(type, LabelSerializer.class, serializerRegistry);
        if (serializer == null) {
            throw new IllegalArgumentException(
                    "No serializer registered for type: " + type.getName()
            );
        }

        return type.cast(serializer.serialize(label));
    }

    @Override
    public @NonNull <T> Label deserialize(
            final @NonNull T serialized
    ) throws DeserializationException {

        final Class<?> type = serialized.getClass();
        final LabelDeserializer<T> deserializer = getConverterSafe(type, LabelDeserializer.class, deserializerRegistry);

        if (deserializer == null) {
            throw new IllegalArgumentException(
                    "No deserializer registered for type: " + type.getName()
            );
        }

        return deserializer.deserialize(serialized, this);
    }

    @Override
    public @NonNull <T> T format(
            final @NonNull String input,
            final @NonNull Class<T> type
    ) throws FormatException {

        final LabelFormatter<T> formatter = getConverterSafe(type, LabelFormatter.class, formaterRegistry);
        if (formatter == null) {
            throw new IllegalArgumentException(
                    "No formatter registered for type: " + type.getName()
            );
        }

        return formatter.format(input);
    }

    @SuppressWarnings("unchecked")
    private <T> @Nullable T getConverterSafe(
            final @NonNull Class<?> type,
            final @NonNull Class<?> converterType,
            final @NonNull Map<Class<?>, ?> registry
    ) {
        final Object converter = registry.get(type);
        if (converter == null) return null;

        if (!converterType.isInstance(converter)) {
            throw new IncompatibleMatchException(type);
        }

        return (T) converter;
    }
}
