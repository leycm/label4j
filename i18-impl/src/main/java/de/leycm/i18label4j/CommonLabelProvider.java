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

import de.leycm.i18label4j.exception.*;
import de.leycm.i18label4j.label.LiteralLabel;
import de.leycm.i18label4j.label.LocaleLabel;
import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.serialize.LabelSerializer;
import de.leycm.i18label4j.source.LocalizationSource;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommonLabelProvider implements LabelProvider {

    public static class Builder {
        private final Map<Class<?>, LabelSerializer<?>> serializerRegistry;
        private MappingRule defaultMappingRule;
        private Locale defaultLocale;

        private Builder() {
            this.serializerRegistry = new ConcurrentHashMap<>();
            this.defaultMappingRule = MappingRule.DOLLAR_CURLY;
            this.defaultLocale = Locale.getDefault();
        }

        public Builder withSerializer(final @NonNull Class<?> type,
                                      final @NonNull LabelSerializer<?> serializer) {
            this.serializerRegistry.put(type, serializer);
            return this;
        }

        public Builder defaultMappingRule(final @NonNull MappingRule rule) {
            this.defaultMappingRule = rule;
            return this;
        }

        public Builder locale(final @NonNull Locale locale) {
            this.defaultLocale = locale;
            return this;
        }

        public CommonLabelProvider build(final @NonNull LocalizationSource source) {
            return new CommonLabelProvider(serializerRegistry, defaultMappingRule, source, defaultLocale);
        }

        public CommonLabelProvider buildWarm(final @NonNull LocalizationSource source,
                                             final @NonNull Locale... locales) {
            CommonLabelProvider provider = new CommonLabelProvider(serializerRegistry,
                    defaultMappingRule, source, defaultLocale);
            provider.warmUp(locales);
            return provider;
        }
    }

    @Contract(value = " -> new", pure = true)
    public static @NonNull Builder builder() {
        return new Builder();
    }

    private final Map<String, Map<String, LocalizedResult>> translationCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, LabelSerializer<?>> serializerRegistry = new ConcurrentHashMap<>();

    private final LocalizationSource localizationSource;
    private final MappingRule defaultMappingRule;
    private final Locale defaultLocale;

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

    @Override
    public @NonNull LocalizationSource getLocalizationSource() {
        return localizationSource;
    }

    @Override
    public @NonNull Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public @NonNull MappingRule getDefaultMappingRule() {
        return defaultMappingRule;
    }

    @Override
    public @NonNull Label createI18Label(
            final @NonNull String key, final @NonNull Function<Locale, String> fallback) {
        return new LocaleLabel(this, key, fallback);
    }

    @Override
    public @NonNull Label createLiteralLabel(final @NonNull String literal) {
        return new LiteralLabel(this, literal);
    }

    @Override
    public @NonNull String translate(final @NonNull Locale locale,
                                     final @NonNull String key,
                                     final @NonNull String fallback
    ) throws NullPointerException, IllegalArgumentException {
        return translate(locale, key).or(fallback);
    }

    @ApiStatus.Internal
    public @NonNull LocalizedResult translate(final @NonNull Locale locale,
                                              final @NonNull String key)
            throws NullPointerException, IllegalArgumentException {

        Map<String, LocalizedResult> localeMap = loadLocaleMap(locale);

        return localeMap.computeIfAbsent(key, k -> {
            if (getDefaultLocale().equals(locale))
                return new LocalizedResult(null);
            return translate(getDefaultLocale(), key);
        });
    }

    @ApiStatus.Internal
    public @NonNull Map<String, LocalizedResult> loadLocaleMap(final @NonNull Locale locale)
            throws NullPointerException, IllegalArgumentException {

        AtomicReference<IllegalArgumentException> loadException = new AtomicReference<>();

        final Map<String, LocalizedResult> localMap = translationCache.computeIfAbsent(locale.toLanguageTag(), tag -> {
            try {
                return localizationSource.getLocalization(locale)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toConcurrentMap(Map.Entry::getKey,
                                e -> new LocalizedResult(e.getValue())));

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

    @Override
    public @NonNull <T> Label deserialize(final @NonNull T serialized)
            throws DeserializationException, IllegalArgumentException, NullPointerException {
        Class<?> type = serialized.getClass();
        LabelSerializer<T> serializer = getSafeSerializer(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        return serializer.deserialize(serialized);
    }

    @Override
    public @NonNull <T> T format(final @NonNull String input, final @NonNull Class<T> type)
            throws FormatException, IllegalArgumentException, NullPointerException {
        LabelSerializer<?> serializer = serializerRegistry.get(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        Object result = serializer.format(input);
        return type.cast(result);
    }

    @SuppressWarnings("unchecked") // cause: we catch ClassCastException explicitly
    private <T> LabelSerializer<T> getSafeSerializer(final @NonNull Class<?> type) {
        try {
            return (LabelSerializer<T>) serializerRegistry.get(type);
        } catch (ClassCastException e) {
            throw new IncompatibleMatchException(type, e);
        }
    }

    @Override
    public void clearCache() {
        translationCache.clear();
    }

    @Override
    public void clearCache(final @NonNull Locale locale) {
        Map<?, ?> localeCache = translationCache.remove(locale.toLanguageTag());
        if (localeCache != null) {localeCache.clear();}
    }

    @Override
    public void clearCache(final @NonNull Locale @NonNull ... locale) {
        for (Locale loc : locale) clearCache(loc);
    }
}