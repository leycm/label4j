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

import de.leycm.label4j.localization.Localization;
import de.leycm.label4j.localization.LocalizationSource;
import de.leycm.label4j.placeholder.PlaceholderRule;
import de.leycm.label4j.serializer.LabelAdapter;
import de.leycm.label4j.serializer.LabelDeserializer;
import de.leycm.label4j.serializer.LabelFormatter;
import de.leycm.label4j.serializer.LabelSerializer;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class LabelProviderImplBuilder {

    // ==== Defaults ==========================================================

    private static final @NonNull PlaceholderRule DEFAULT_RULE   = PlaceholderRule.DEFAULT;
    private static final @NonNull Locale          DEFAULT_LOCALE = Locale.getDefault();

    // ==== Builder State =====================================================

    private final @NonNull Map<Class<?>, LabelSerializer<?>>   serializers   = new HashMap<>();
    private final @NonNull Map<Class<?>, LabelDeserializer<?>> deserializers = new HashMap<>();
    private final @NonNull Map<Class<?>, LabelFormatter<?>>    formatters    = new HashMap<>();

    private @NonNull PlaceholderRule placeholderRule = DEFAULT_RULE;
    private @NonNull Locale          defaultLocale   = DEFAULT_LOCALE;

    private @NonNull Consumer<Exception> loadErrorHandler = e -> {};
    private @NonNull Function<Localization, String> fallbackHandler = l -> "!" + l.key();

    // package-private: use LabelProviderImpl.builder()
    LabelProviderImplBuilder() {}

    // ==== Configuration Methods =============================================

    /**
     * Sets the default {@link Locale} used when no locale is specified on a
     * resolution call.
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder locale(
            final @NonNull Locale locale
    ) {
        this.defaultLocale = locale;
        return this;
    }

    /**
     * Sets the {@link PlaceholderRule} used for placeholder substitution.
     * Defaults to {@link PlaceholderRule#DEFAULT} ({@code ${variable}}).
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder placeholderRule(
            final @NonNull PlaceholderRule rule
    ) {
        this.placeholderRule = rule;
        return this;
    }

    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder onLoadError(
            final @NonNull Consumer<Exception> handler
    ) {
        this.loadErrorHandler = handler;
        return this;
    }

    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder onFallback(
            final @NonNull Function<Localization, String> handler
    ) {
        this.fallbackHandler = handler;
        return this;
    }

    /**
     * Registers a {@link LabelSerializer} for the given target type.
     */
    @Contract("_, _ -> this")
    public @NonNull <T> LabelProviderImplBuilder withSerializer(
            final @NonNull Class<T> type,
            final @NonNull LabelSerializer<T> serializer
    ) {
        this.serializers.put(type, serializer);
        return this;
    }

    /**
     * Registers multiple serializers at once from an existing map.
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder withSerializers(
            final @NonNull Map<Class<?>, LabelSerializer<?>> serializers
    ) {
        this.serializers.putAll(serializers);
        return this;
    }

    /**
     * Registers a {@link LabelDeserializer} for the given target type.
     */
    @Contract("_, _ -> this")
    public @NonNull <T> LabelProviderImplBuilder withDeserializer(
            final @NonNull Class<T> type,
            final @NonNull LabelDeserializer<T> deserializer
    ) {
        this.deserializers.put(type, deserializer);
        return this;
    }

    /**
     * Registers multiple deserializers at once from an existing map.
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder withDeserializers(
            final @NonNull Map<Class<?>, LabelDeserializer<?>> deserializers
    ) {
        this.deserializers.putAll(deserializers);
        return this;
    }

    /**
     * Registers a {@link LabelFormatter} for the given target type.
     */
    @Contract("_, _ -> this")
    public @NonNull <T> LabelProviderImplBuilder withFormatter(
            final @NonNull Class<T> type,
            final @NonNull LabelFormatter<T> formater
    ) {
        this.formatters.put(type, formater);
        return this;
    }

    /**
     * Registers multiple formatters at once from an existing map.
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder withFormatters(
            final @NonNull Map<Class<?>, LabelFormatter<?>> formatters
    ) {
        this.formatters.putAll(formatters);
        return this;
    }

    /**
     * Registers a {@link LabelAdapter} for the given target type.
     * Convenience method that registers the adapter as both serializer
     * and deserializer at once.
     */
    @Contract("_, _ -> this")
    public @NonNull <T> LabelProviderImplBuilder withAdapter(
            final @NonNull Class<T> type,
            final @NonNull LabelAdapter<T> adapter) {
        this.serializers.put(type, adapter);
        this.deserializers.put(type, adapter);
        return this;
    }

    /**
     * Registers multiple adapters at once.
     */
    @Contract("_ -> this")
    public @NonNull <T> LabelProviderImplBuilder withAdapters(
            final @NonNull Map<Class<?>, LabelAdapter<?>> adapters) {
        this.serializers.putAll(adapters);
        this.deserializers.putAll(adapters);
        return this;
    }

    // ==== Build Methods =====================================================

    /**
     * Constructs a new {@link LabelProviderImpl} with the configured settings
     * and the given {@link LocalizationSource}.
     */
    @Contract("_ -> new")
    public @NonNull LabelProviderImpl build(final @NonNull LocalizationSource source) {
        return new LabelProviderImpl(
                new ConcurrentHashMap<>(serializers),
                new ConcurrentHashMap<>(deserializers),
                new ConcurrentHashMap<>(formatters),
                placeholderRule,
                source,
                defaultLocale,
                loadErrorHandler,
                fallbackHandler
        );
    }

    /**
     * Constructs a new {@link LabelProviderImpl} and immediately warms the
     * translation cache for the given locales.
     */
    @Contract("_, _ -> new")
    public @NonNull LabelProviderImpl buildWarm(
            final @NonNull LocalizationSource source,
            final @NonNull Locale @NonNull ... locales) {
        final LabelProviderImpl provider = build(source);
        provider.warmup(locales);
        return provider;
    }
}