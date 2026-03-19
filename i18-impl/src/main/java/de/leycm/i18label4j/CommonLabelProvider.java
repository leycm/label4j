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
import org.jetbrains.annotations.Contract;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class CommonLabelProvider implements LabelProvider {

    public static class Builder {
        private final Map<Class<?>, LabelSerializer<?>> serializerRegistry;
        private MappingRule defaultMappingRule;
        private Locale defaultLocale;

        private Builder() {
            this.serializerRegistry = new ConcurrentHashMap<>();
            this.defaultMappingRule = MappingRule.FSTRING;
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

    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();
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
    public @NonNull Label createI18Label(@NonNull String key, @NonNull Function<Locale, String> fallback) {
        return new LocaleLabel(this, key, fallback);
    }

    @Override
    public @NonNull Label createLiteralLabel(@NonNull String literal) {
        return new LiteralLabel(this, literal);
    }

    @Override
    public @NonNull String translate(@NonNull String key, @NonNull Locale locale,
                                     @NonNull Function<Locale, String> fallback) {

        final AtomicReference<RuntimeException> exception = new AtomicReference<>();

        final Locale defaultLocale = getDefaultLocale();
        final String localeTag = locale.toLanguageTag();
        final String defaultTag = defaultLocale.toLanguageTag();

        Map<String, String> localeMap = translationCache.computeIfAbsent(
                localeTag,
                tag -> getLocalization(locale, exception)
        );

        String value = localeMap.get(key);
        if (value != null) {
            if (exception.get() != null) {
                throwCachedException(localeTag, exception);
            }
            return value;
        }

        if (!locale.equals(defaultLocale)) {

            Map<String, String> defaultMap = translationCache.computeIfAbsent(
                    defaultTag,
                    tag -> getLocalization(defaultLocale, exception)
            );

            String defaultValue = defaultMap.get(key);

            if (defaultValue != null) {
                localeMap.putIfAbsent(key, defaultValue);

                if (exception.get() != null) {
                    throwCachedException(localeTag, exception);
                }

                return defaultValue;
            }
        }

        String fallbackValue = fallback.apply(locale);
        localeMap.putIfAbsent(key, fallbackValue);

        if (exception.get() != null) {
            throwCachedException(localeTag, exception);
        }

        return fallbackValue;
    }

    @Contract("_, _ -> new")
    private @NonNull Map<String, String> getLocalization(@NonNull Locale locale,
                                                         @NonNull AtomicReference<RuntimeException> exception)
            throws NullPointerException {
        try {
            Map<String, String> translations = localizationSource.getLocalization(locale);
            return new ConcurrentHashMap<>(translations);
        } catch (Exception e) {
            exception.set(new RuntimeException("Failed to load translations for locale: " + locale.toLanguageTag(), e));
            return new ConcurrentHashMap<>();
        }
    }

    @Contract("_, _ -> fail")
    private void throwCachedException(final @NonNull String localeTag,
                                      final @NonNull AtomicReference<RuntimeException> exception)
            throws NullPointerException, IllegalArgumentException  {
        throw new IllegalArgumentException(
                "Failed to load translations for locale \"" + localeTag
                        + "\"; an empty map is cached due to the failure, "
                        + "translation attempts will not re-attempt loading"
                        + "until the cache is cleared",
                exception.get()
        );
    }

    @Override
    public @NonNull <T> T serialize(final @NonNull Label label,
                                    final @NonNull Class<T> type)
            throws SerializationException, IllegalArgumentException {
        LabelSerializer<?> serializer = serializerRegistry.get(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        Object result = serializer.serialize(label);
        return type.cast(result);
    }

    @Override
    public @NonNull <T> Label deserialize(@NonNull T serialized)
            throws DeserializationException, IllegalArgumentException  {
        Class<?> type = serialized.getClass();
        LabelSerializer<T> serializer = getSafeSerializer(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        return serializer.deserialize(serialized);
    }

    @Override
    public @NonNull <T> T format(@NonNull String input, @NonNull Class<T> type)
            throws FormatException, IllegalArgumentException  {
        LabelSerializer<?> serializer = serializerRegistry.get(type);

        if (serializer == null)
            throw new IllegalArgumentException("Unsupported serialization type: " + type.getName());

        Object result = serializer.format(input);
        return type.cast(result);
    }

    @SuppressWarnings("unchecked") // because: we try catch it
    private <T> LabelSerializer<T> getSafeSerializer(Class<?> type) {
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
    public void clearCache(@NonNull Locale locale) {
        translationCache.computeIfPresent(locale.toLanguageTag(),
                (tag, map) -> {
            map.clear();
            return map;
        });
    }

    @Override
    public void clearCache(@NonNull Locale @NonNull ... locale) {
        for (Locale loc : locale) clearCache(loc);
    }

}
