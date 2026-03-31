package de.leycm.i18label4j;

import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.serializer.LabelSerializer;
import de.leycm.i18label4j.source.LocalizationSource;
import lombok.NonNull;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fluent builder for {@link CommonLabelProvider} instances.
 *
 * <p>Provides a readable, step-by-step construction API. After
 * setting all desired options call {@link #build(LocalizationSource)}
 * to create the provider, or {@link #buildWarm(LocalizationSource, Locale...)}
 * to additionally warm up the cache for the specified locales.</p>
 *
 * <p>Thread Safety: Builder instances are not thread-safe and must
 * not be shared across threads.</p>
 *
 * @since 1.0
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class CommonLabelProviderBuilder {

    // serializer registry accumulated during builder configuration
    private final Map<Class<?>, LabelSerializer<?>> serializerRegistry;
    private MappingRule defaultMappingRule;
    private Locale defaultLocale;

    CommonLabelProviderBuilder() {
        this.serializerRegistry = new ConcurrentHashMap<>();
        this.defaultMappingRule = MappingRule.DOLLAR_CURLY;
        this.defaultLocale = Locale.getDefault();
    }

    /**
     * Registers a {@link LabelSerializer} for the given target type.
     *
     * <p>If a serializer was previously registered for {@code type}
     * it is silently replaced.</p>
     *
     * @param type       the target class this serializer handles;
     *                   never {@code null}
     * @param serializer the serializer implementation;
     *                   never {@code null}
     * @return this builder for method chaining; never {@code null}
     * @throws NullPointerException if {@code type} or {@code serializer}
     *                              is {@code null}
     */
    public @NonNull CommonLabelProviderBuilder withSerializer(final @NonNull Class<?> type,
                                           final @NonNull LabelSerializer<?> serializer) {
        this.serializerRegistry.put(type, serializer);
        return this;
    }

    /**
     * Sets the default {@link MappingRule} used when
     * {@link Label#resolve()} is called without an explicit rule.
     *
     * <p>Defaults to {@link MappingRule#DOLLAR_CURLY} if not set.</p>
     *
     * @param rule the mapping rule; never {@code null}
     * @return this builder for method chaining; never {@code null}
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    public @NonNull CommonLabelProviderBuilder defaultMappingRule(final @NonNull MappingRule rule) {
        this.defaultMappingRule = rule;
        return this;
    }

    /**
     * Sets the default {@link Locale} used when no locale is
     * specified in label resolution calls.
     *
     * <p>Defaults to {@link Locale#getDefault()} if not set.</p>
     *
     * @param locale the default locale; never {@code null}
     * @return this builder for method chaining; never {@code null}
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    public @NonNull CommonLabelProviderBuilder locale(final @NonNull Locale locale) {
        this.defaultLocale = locale;
        return this;
    }

    /**
     * Builds the {@link CommonLabelProvider} with the configured settings
     * and the given {@link LocalizationSource}.
     *
     * @param source the localization source to use; never {@code null}
     * @return a new {@link CommonLabelProvider}; never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public @NonNull CommonLabelProvider build(final @NonNull LocalizationSource source) {
        return new CommonLabelProvider(serializerRegistry, defaultMappingRule, source, defaultLocale);
    }

    /**
     * Builds the {@link CommonLabelProvider} and immediately warms the
     * translation cache for each of the specified locales.
     *
     * <p>Equivalent to calling {@link #build(LocalizationSource)} followed
     * by {@link LabelProvider#warmUp(Locale...)}.</p>
     *
     * @param source  the localization source to use; never {@code null}
     * @param locales the locales to preload; never {@code null},
     *                individual elements never {@code null}
     * @return a new, pre-warmed {@link CommonLabelProvider}; never {@code null}
     * @throws IllegalArgumentException if any locale's translation data
     *                                  cannot be loaded from the source
     * @throws NullPointerException     if {@code source} or {@code locales}
     *                                  is {@code null} or contains {@code null}
     */
    public @NonNull CommonLabelProvider buildWarm(final @NonNull LocalizationSource source,
                                                  final @NonNull Locale... locales) {
        CommonLabelProvider provider = new CommonLabelProvider(serializerRegistry,
                defaultMappingRule, source, defaultLocale);
        provider.warmUp(locales);
        return provider;
    }
}