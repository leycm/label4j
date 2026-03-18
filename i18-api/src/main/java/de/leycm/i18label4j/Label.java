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

import de.leycm.i18label4j.mapping.Mapping;
import de.leycm.i18label4j.mapping.MappingRule;

import lombok.NonNull;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public interface Label {

    @NonNull LabelProvider provider();

    @NonNull Set<Mapping> mappings();

    default @NonNull Label mapTo(final @NonNull String key,
                                 final @NonNull Object value) {
        return mapTo(key, () -> value);
    }

    default @NonNull Label mapTo(final @NonNull String key,
                                 final @NonNull Supplier<Object> supplier) {
        return mapTo(provider().getDefaultMappingRule(), key, supplier);
    }

    default @NonNull Label mapTo(final @NonNull MappingRule rule,
                                 final @NonNull String key,
                                 final @NonNull Supplier<Object> supplier) {
        return mapTo(new Mapping(rule, key, () -> String.valueOf(supplier.get())));
    }

    @NonNull Label mapTo(final @NonNull Mapping mapping);

    default @NonNull String in() {
        return in(provider().getDefaultLocale());
    }

    default <T> @NonNull T in(final @NonNull Class<T> type) {
        return in(provider().getDefaultLocale(), type);
    }

    default <T> @NonNull T in(@NonNull Locale locale, final @NonNull Class<T> type) {
        return provider().format(in(locale), type);
    }

    @NonNull String in(@NonNull Locale locale);

    default @NonNull String mapped() {
        return mapped(provider().getDefaultLocale());
    }

    default <T> @NonNull T mapped(final @NonNull Class<T> type) {
        return mapped(provider().getDefaultLocale(), type);
    }

    default <T> @NonNull T mapped(final @NonNull Locale locale, final @NonNull Class<T> type) {
        return provider().format(mapped(locale), type);
    }

    default @NonNull String mapped(@NonNull Locale locale) {
        return Mapping.apply(mappings(), in(locale));
    }

    default @NonNull String serialize() {
        return provider().serialize(this, String.class);
    }

    @Override
    @NonNull String toString();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
