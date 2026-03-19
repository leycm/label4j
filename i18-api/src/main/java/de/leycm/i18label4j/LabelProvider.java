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
import de.leycm.i18label4j.mapping.MappingRule;
import de.leycm.i18label4j.source.LocalizationSource;
import de.leycm.init4j.instance.Instanceable;

import lombok.NonNull;

import java.util.Locale;
import java.util.function.Function;

public interface LabelProvider extends Instanceable {

    /**
     * Returns the singleton instance of the {@link LabelProvider}.
     *
     * <p>This method relies on the {@link Instanceable#getInstance(Class)}
     * mechanism to retrieve the registered implementation.</p>
     *
     * <p>The provider must be initialized via {@link Instanceable#register(Instanceable, Class)} before first use
     * to ensure proper configuration and resource loading.</p>
     *
     * @return the singleton instance of {@link LabelProvider}; never {@code null}
     * @throws NullPointerException if no implementation is registered
     */
    static @NonNull LabelProvider getInstance() {
        return Instanceable.getInstance(LabelProvider.class);
    }

    @NonNull LocalizationSource getLocalizationSource();

    @NonNull Locale getDefaultLocale();

    @NonNull MappingRule getDefaultMappingRule();

    default void warmUp(final @NonNull Locale @NonNull ... localizations) {
        for (Locale locale : localizations) {
            // note: using the key "__warmup__" is a bit hacky, but it dont matter because it is handled normally anyway
            translate("__warmup__", locale, createFallback("__warmup__"));
        }
    }

    default @NonNull Function<Locale, String> createFallback(final @NonNull String key) {
        return locale -> "[" + locale.toLanguageTag().toLowerCase() + "/" + key + "]";
    }

    @NonNull Label createI18Label(@NonNull String key, @NonNull Function<Locale, String> fallback);

    @NonNull Label createLiteralLabel(@NonNull String literal);

    @NonNull String translate(@NonNull String key, @NonNull Locale locale,
                              @NonNull Function<Locale, String> fallback);

    <T> @NonNull T serialize(@NonNull Label label, @NonNull Class<T> type)
            throws SerializationException;

    <T> @NonNull Label deserialize(@NonNull T serialized)
            throws DeserializationException;

    <T> @NonNull T format(@NonNull String input, @NonNull Class<T> type)
            throws FormatException;

    void clearCache();

    void clearCache(@NonNull Locale locale);

    void clearCache(@NonNull Locale @NonNull ... locale);
}
