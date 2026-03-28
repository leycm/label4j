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
package de.leycm.i18label4j.label;

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.mapping.Mapping;
import de.leycm.i18label4j.mapping.MappingRule;

import de.leycm.i18label4j.serializer.Localization;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A {@link Label} implementation that resolves text through the
 * {@link LabelProvider}'s translation system.
 *
 * <p>{@link LocaleLabel} holds a translation key and a fallback
 * {@link Function} that is invoked when no translation can be found for
 * the requested {@link Locale} or the provider's default locale. Each
 * call to {@link #in(Locale)} delegates to
 * {@link LabelProvider#translate(Locale, String, String)}, meaning the
 * provider's internal cache and locale-fallback logic apply
 * transparently.</p>
 *
 * <p>Placeholder {@link Mapping} objects may be registered on the label
 * and are applied by the default
 * {@link MappingRule} when
 * {@link #mapped(Locale)} is called.</p>
 *
 * <p>Equality is based on the combination of owning provider and
 * translation key, so two locale labels with the same key and provider
 * are considered equal regardless of their fallback functions.</p>
 *
 * <p>Thread Safety: This class is not thread-safe. The mutable
 * {@code mappings} set is not synchronized; instances must not be
 * modified and accessed from multiple threads concurrently.</p>
 *
 * @since 1.0
 * @see Label
 * @see LabelProvider#createI18Label(String, Function)
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
@SuppressWarnings("ClassCanBeRecord") // cause: mutable mappings
public class LocaleLabel implements Label {

    // the owning label provider
    private final @NonNull LabelProvider provider;
    // the set of placeholder mappings
    private final @NonNull Set<Mapping> mappings;
    // the translation key for lookups
    private final @NonNull String key;
    // the fallback function for missing translations
    private final @NonNull Function<Locale, String> fallback;

    /**
     * Constructs a new {@link LocaleLabel} with an empty mapping set.
     *
     * @param provider the owning provider; must not be {@code null}
     * @param key      the translation key; must not be {@code null}
     * @param fallback the function that produces a fallback string when
     *                 no translation is available; must not be {@code null}
     */
    public LocaleLabel(final @NonNull LabelProvider provider,
                       final @NonNull String key,
                       final @NonNull Function<Locale, String> fallback) {
        this(provider, ConcurrentHashMap.newKeySet(), key, fallback);
    }

    /**
     * Constructs a new {@link LocaleLabel} with an existing mapping set.
     *
     * <p>This constructor is intended for internal use only, for example
     * when cloning an existing label with pre-populated mappings.</p>
     *
     * @param provider the owning provider; must not be {@code null}
     * @param mappings the initial set of mappings; must not be {@code null}
     * @param key      the translation key; must not be {@code null}
     * @param fallback the function that produces a fallback string when
     *                 no translation is available; must not be {@code null}
     */
    @ApiStatus.Internal
    public LocaleLabel(final @NonNull LabelProvider provider,
                       final @NonNull Set<Mapping> mappings,
                       final @NonNull String key,
                       final @NonNull Function<Locale, String> fallback) {
        // note: using a concurrent set to be thread-safe
        final Set<Mapping> set = ConcurrentHashMap.newKeySet();
        set.addAll(mappings);

        this.provider = provider;
        this.mappings = set;
        this.key = key;
        this.fallback = fallback;
    }

    // ==== Basic Accessors ===================================================

    /**
     * Returns the owning {@link LabelProvider}.
     *
     * @return the provider; never {@code null}
     */
    @Override
    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    /**
     * Returns an unmodifiable view of all registered {@link Mapping} objects.
     *
     * @return the mappings; never {@code null}, may be empty
     */
    @Override
    public @NonNull Set<Mapping> getMappings() {
        return Collections.unmodifiableSet(mappings);
    }

    /**
     * Returns the translation key used for lookups.
     *
     * @return the translation key; never {@code null}
     */
    public @NonNull String getKey() {
        return key;
    }

    /**
     * Returns the fallback for the default locale, used
     * when no translation is found for the requested
     * locale or the provider's default locale.
     *
     * @return the fallback value for the default Local; never {@code null}
     */
    public @NonNull String getFallback() {
        return fallback.apply(provider.getDefaultLocale());
    }

    // ==== Mapping Registration ===============================================

    /**
     * {@inheritDoc}
     *
     * @param mapping the mapping to register; must not be {@code null}
     * @return this label for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     * @throws NullPointerException     if {@code mapping} is {@code null}
     */
    @Override
    public @NonNull Label mapTo(final @NonNull Mapping mapping) throws IllegalArgumentException {
        if (mappings.contains(mapping))
            throw new IllegalArgumentException(
                    "Mapping with key \"" + mapping.key() + "\" already exists for this label.");
        mappings.add(mapping);
        return this;
    }

    // ==== Resolution ========================================================

    /**
     * Looks up the translation for the given locale via the
     * {@link LabelProvider}.
     *
     * <p>The fallback function is evaluated and passed to
     * {@link LabelProvider#translate(Locale, String, String)} so that
     * the provider applies its own locale-fallback logic before
     * resorting to the function's result.</p>
     *
     * @param locale the target locale; must not be {@code null}
     * @return the translated string or the fallback value;
     *         never {@code null}
     * @throws IllegalArgumentException if the locale's translation data
     *                                  cannot be loaded from the source
     */
    @Override
    public @NonNull String in(final @NonNull Locale locale) {
        return localized().or(fallback.apply(locale));
    }

    @Override
    public @NonNull Localization localized(@NonNull Locale locale) {
        return provider.translate(locale, key);
    }

    // ==== Object Methods ===================================================

    /**
     * Returns a string representation of this label.
     *
     * <p>Attempts to serialize the label to a {@link String} via the
     * registered serializer. If serialization is unavailable or throws,
     * falls back to a synthetic identity string of the form
     * {@code LocaleLabel@<hashCode>}.</p>
     *
     * @return a non-{@code null} string representation
     */
    @Override
    public @NonNull String toString() {
        try {
            return serialize(String.class);
        } catch (Throwable e) {
            return getClass().getSimpleName() +
                    "@" + Integer.toHexString(hashCode());
        }
    }

    /**
     * Determines equality based on provider identity and translation key.
     *
     * <p>Two {@link LocaleLabel} instances are equal when they share the
     * same {@link LabelProvider} and the same translation key, regardless
     * of their fallback functions.</p>
     *
     * @param obj the object to compare; may be {@code null}
     * @return {@code true} if {@code obj} is a {@link LocaleLabel} with
     *         an equal provider and key
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LocaleLabel that = (LocaleLabel) obj;
        return provider.equals(that.getProvider()) &&
                key.equals(that.getKey());
    }

    /**
     * Returns a hash code derived from the provider and key,
     * consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(provider, key);
    }
}