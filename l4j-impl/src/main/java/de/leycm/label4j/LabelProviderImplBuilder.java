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

import de.leycm.label4j.localization.LocalizationSource;
import de.leycm.label4j.placeholder.PlaceholderRule;
import de.leycm.label4j.serializer.LabelSerializer;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fluent builder for {@link LabelProviderImpl}.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * LabelProvider provider = LabelProviderImpl.builder()
 *     .locale(Locale.ENGLISH)
 *     .placeholderRule(PlaceholderRule.DOLLAR_CURLY)
 *     .withSerializer(String.class, myStringSerializer)
 *     .build(source);
 * }</pre>
 *
 * <p>All setter methods return {@code this} for chaining. Neither
 * {@link #build(LocalizationSource)} nor {@link #buildWarm(LocalizationSource, Locale...)}
 * mutates the builder, so the same builder instance can produce multiple
 * providers if needed.</p>
 *
 * @since 2.0
 * @see LabelProviderImpl
 */
public class LabelProviderImplBuilder {

    // ==== Defaults ==========================================================

    private static final @NonNull PlaceholderRule DEFAULT_RULE   = PlaceholderRule.DEFAULT;
    private static final @NonNull Locale          DEFAULT_LOCALE = Locale.getDefault();

    // ==== Builder State =====================================================

    private final @NonNull Map<Class<?>, LabelSerializer<?>> serializers = new HashMap<>();

    private @NonNull PlaceholderRule placeholderRule = DEFAULT_RULE;
    private @NonNull Locale          defaultLocale   = DEFAULT_LOCALE;

    // package-private: use LabelProviderImpl.builder()
    LabelProviderImplBuilder() {}

    // ==== Configuration Methods =============================================

    /**
     * Sets the default {@link Locale} used when no locale is specified on a
     * resolution call.
     *
     * @param locale the default locale; never {@code null}
     * @return {@code this} builder
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder locale(final @NonNull Locale locale) {
        this.defaultLocale = locale;
        return this;
    }

    /**
     * Sets the {@link PlaceholderRule} used for placeholder substitution.
     * Defaults to {@link PlaceholderRule#DEFAULT} ({@code ${variable}}).
     *
     * @param rule the placeholder rule; never {@code null}
     * @return {@code this} builder
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder placeholderRule(
            final @NonNull PlaceholderRule rule) {
        this.placeholderRule = rule;
        return this;
    }

    /**
     * Registers a {@link LabelSerializer} for the given target type.
     *
     * <p>If a serializer for {@code type} is already registered it will be
     * silently replaced.</p>
     *
     * @param <T>        the target type
     * @param type       the class to register the serializer for; never {@code null}
     * @param serializer the serializer; never {@code null}
     * @return {@code this} builder
     */
    @Contract("_, _ -> this")
    public @NonNull <T> LabelProviderImplBuilder withSerializer(
            final @NonNull Class<T> type,
            final @NonNull LabelSerializer<T> serializer) {
        this.serializers.put(type, serializer);
        return this;
    }

    /**
     * Registers multiple serializers at once from an existing map.
     *
     * @param serializers map of type -> serializer entries; never {@code null}
     * @return {@code this} builder
     */
    @Contract("_ -> this")
    public @NonNull LabelProviderImplBuilder withSerializers(
            final @NonNull Map<Class<?>, LabelSerializer<?>> serializers) {
        this.serializers.putAll(serializers);
        return this;
    }

    // ==== Build Methods =====================================================

    /**
     * Constructs a new {@link LabelProviderImpl} with the configured settings
     * and the given {@link LocalizationSource}.
     *
     * <p>The translation cache starts empty; locales are loaded lazily on
     * first access.</p>
     *
     * @param source the localization source; never {@code null}
     * @return a new {@link LabelProviderImpl}; never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @Contract("_ -> new")
    public @NonNull LabelProviderImpl build(final @NonNull LocalizationSource source) {
        return new LabelProviderImpl(
                new ConcurrentHashMap<>(serializers),
                placeholderRule,
                source,
                defaultLocale
        );
    }

    /**
     * Constructs a new {@link LabelProviderImpl} and immediately warms the
     * translation cache for the given locales.
     *
     * <p>This is equivalent to calling {@link #build(LocalizationSource)}
     * followed by {@link LabelProviderImpl#warmup(Locale...)}.</p>
     *
     * @param source  the localization source; never {@code null}
     * @param locales the locales to preload; never {@code null},
     *                individual elements never {@code null}
     * @return a new, warmed {@link LabelProviderImpl}; never {@code null}
     * @throws NullPointerException if {@code source} or any locale is {@code null}
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
