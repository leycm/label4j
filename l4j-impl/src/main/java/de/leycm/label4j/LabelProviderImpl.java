/*
 * Copyright (C) 2026 leycm <leycm@proton.me>
 *
 * This file is part of label4j.
 *
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
import de.leycm.label4j.localization.Localization;
import de.leycm.label4j.localization.LocalizationSource;
import de.leycm.label4j.placeholder.PlaceholderRule;
import de.leycm.label4j.serializer.LabelDeserializer;
import de.leycm.label4j.serializer.LabelFormater;
import de.leycm.label4j.serializer.LabelSerializer;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class LabelProviderImpl implements LabelProvider {

    // ==== Builder ===========================================================

    @Contract(value = " -> new", pure = true)
    public static @NonNull LabelProviderImplBuilder builder() {
        return new LabelProviderImplBuilder();
    }

    // todo: add more simple constructors

    // ==== Instance State ===================================================

    // locale language-tag -> (translation key -> LocalizedResult)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Localization>>
            translationCache = new ConcurrentHashMap<>();
    // target class -> serializer
    private final ConcurrentHashMap<Class<?>, LabelSerializer<?>>
            serializerRegistry = new ConcurrentHashMap<>();

    private final @NonNull LocalizationSource localizationSource;
    private final @NonNull PlaceholderRule placeholderRule;
    private final @NonNull Locale defaultLocale;

    public LabelProviderImpl(
            final @NonNull Map<Class<?>, LabelSerializer<?>> serializers,
            final @NonNull PlaceholderRule placeholderRule,
            final @NonNull LocalizationSource source,
            final @NonNull Locale defaultLocale) {
        this.serializerRegistry.putAll(serializers);
        this.localizationSource = source;
        this.placeholderRule = placeholderRule;
        this.defaultLocale = defaultLocale;
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
        return localization.orElseGet(() -> handleFallback(localization.key()));
    }

    @Override
    public String handleFallback(final @NonNull String key) {
        return "!" + key + "!";
    }

    @Override
    public void warmup(@NonNull Locale @NonNull ... locales) {
        for (final Locale locale : locales) {
            final Localization localization = localize(locale, Localization.WARMUP_KEY);
            // todo: handle localization
        }
    }

    // ==== Localization ======================================================

    @Override
    public @NonNull Localization localize(
            final @NonNull Locale locale,
            final @NonNull String key) {

        final ConcurrentMap<String, Localization> localeMap = loadLocaleMap(locale);

        final Localization existing = localeMap.get(key);
        if (existing != null) return existing;

        if (defaultLocale.equals(locale)) {
            final Localization empty = Localization.empty(key, locale);
            localeMap.putIfAbsent(key, empty);
            return empty;
        }

        final Localization fallback = localize(defaultLocale, key);

        // note: race window between get() above and putIfAbsent() is harmless
        //       at worst a duplicate Localization object is created and discarded.
        return localeMap.computeIfAbsent(key, k -> fallback);
    }

    @ApiStatus.Internal
    public @NonNull ConcurrentMap<String, Localization> loadLocaleMap(
            final @NonNull Locale locale) {

        final AtomicReference<IllegalArgumentException> loadException = new AtomicReference<>();

        final Function<Throwable, IllegalArgumentException>
                loadExceptionBuilder = e -> new IllegalArgumentException(
                        "Failed to load translations for locale \""
                                + locale.toLanguageTag()
                                + "\"; an empty map is cached due to "
                                + "the failure. Translation"
                                + "attempts will not re-attempt loading "
                                + "until the cache is cleared.", e
                );


        final ConcurrentMap<String, Localization> localeMap =
                translationCache.computeIfAbsent(locale.toLanguageTag(),
                        tag -> {
                    try {
                        return new ConcurrentHashMap<>(
                                localizationSource.getLocalization(locale)
                        );
                    } catch (final Exception e) {
                        loadException.set(loadExceptionBuilder.apply(e));
                        return new ConcurrentHashMap<>();
                    }
                });

        final IllegalArgumentException err = loadException.get();
        if (err != null) throw err;

        return localeMap;
    }

    // ==== Serialization =====================================================

    @Override
    public @NonNull <T> T serialize(
            final @NonNull Label label,
            final @NonNull Class<T> type)
            throws SerializationException {

        final LabelSerializer<?> serializer = serializerRegistry.get(type);
        if (serializer == null)
            throw new IllegalArgumentException(
                    "No serializer registered for type: " + type.getName()
            );

        final Object result = serializer.serialize(label);
        return type.cast(result);
    }

    @Override
    public @NonNull <T> Label deserialize(final @NonNull T serialized)
            throws DeserializationException {

        final Class<?> type = serialized.getClass();
        final LabelSerializer<T> serializer = getSafeSerializer(type);

        if (serializer == null)
            throw new IllegalArgumentException(
                    "No serializer registered for type: " + type.getName()
            );

        if (serializer instanceof LabelDeserializer) {
            @SuppressWarnings("unchecked")
            final LabelDeserializer<T> deserializer = (LabelDeserializer<T>) serializer;
            return deserializer.deserialize(serialized, this);
        }

        throw new DeserializationException(serialized,
                "Registered serializer for " + type.getName()
                        + " does not implement LabelDeserializer"
        );
    }

    @Override
    public @NonNull <T> T format(
            final @NonNull String input,
            final @NonNull Class<T> type)
            throws FormatException {

        final LabelSerializer<?> serializer = serializerRegistry.get(type);
        if (serializer == null)
            throw new IllegalArgumentException(
                    "No serializer registered for type: " + type.getName()
            );

        if (serializer instanceof LabelFormater) {
            @SuppressWarnings("unchecked")
            final LabelFormater<T> formatter = (LabelFormater<T>) serializer;
            return formatter.format(input);
        }

        throw new FormatException(input, type,
                new UnsupportedOperationException(
                        "Registered serializer for " + type.getName()
                                + " does not implement LabelFormater"
                )
        );
    }

    @SuppressWarnings("unchecked") // ClassCastException is caught explicitly
    private <T> LabelSerializer<T> getSafeSerializer(final @NonNull Class<?> type) {
        try {
            return (LabelSerializer<T>) serializerRegistry.get(type);
        } catch (final ClassCastException e) {
            throw new IncompatibleMatchException(type, e);
        }
    }
}
