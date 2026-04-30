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

import de.leycm.label4j.exception.DuplicatePlaceholderException;
import de.leycm.label4j.label.ConstantLabel;
import de.leycm.label4j.label.LiteralLabel;
import de.leycm.label4j.label.LocaleLabel;
import de.leycm.label4j.localization.Localization;
import de.leycm.label4j.placeholder.Placeholder;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public interface Label {
    // ==== Static Constructor ================================================

    static @NonNull Label constant(final @NonNull String key) {
        return new ConstantLabel(LabelProvider.getInstance(), key);
    }

    static @NonNull Label constant(
            final @NonNull String key,
            final @NonNull LabelProvider provider
    ) {
        return new ConstantLabel(provider, key);
    }

    static @NonNull Label locale(final @NonNull String key) {
        return new LocaleLabel(LabelProvider.getInstance(), key);
    }

    static @NonNull Label locale(
            final @NonNull String key,
            final @NonNull LabelProvider provider
    ) {
        return new LocaleLabel(provider, key);
    }

    static @NonNull Label literal(final @NonNull String literal) {
        return new LiteralLabel(LabelProvider.getInstance(), literal);
    }

    static @NonNull Label literal(
            final @NonNull String literal,
            final @NonNull LabelProvider provider
    ) {
        return new LiteralLabel(provider, literal);
    }

    static <T> @NonNull LabelFactory<T> factory() {
        return new LabelFactory<>(LabelProvider.getInstance());
    }

    static <T> @NonNull LabelFactory<T> factory(
            final @NonNull LabelProvider provider
    ) {
        return new LabelFactory<>(provider);
    }

    // ==== Field Methods =====================================================

    @NonNull LabelProvider getProvider();

    @UnmodifiableView @NonNull Set<Placeholder> getPlaceholders();

    // ==== Replacement Methods ===============================================

    default @NonNull Label replace(
            final @NonNull String key,
            final @NonNull Object value
    ) throws DuplicatePlaceholderException {
        return replace(key, () -> value);
    }

    default @NonNull Label replace(
            final @NonNull String key,
            final @NonNull Supplier<@Nullable Object> supplier
    ) throws DuplicatePlaceholderException {
        return replace(new Placeholder(key, supplier));
    }

    default @NonNull Label replace(
            @NonNull Placeholder @NonNull ... placeholders
    ) throws DuplicatePlaceholderException {
        return replace(Set.of(placeholders));
    }

    @NonNull Label replace(@NonNull Set<@NonNull Placeholder> placeholders)
            throws DuplicatePlaceholderException;

    // ==== Resolution Methods ================================================

    default @NonNull <T> T resolveDefault(final @NonNull Class<T> type) {
        return resolve(getProvider().getDefaultLocale(), type);
    }

    default @NonNull String resolveDefault() {
        return resolve(getProvider().getDefaultLocale());
    }

    default @NonNull Localization localizeDefault() {
        return localize(getProvider().getDefaultLocale());
    }

    default @NonNull <T> T resolve(final @NonNull Locale locale,
                                   final @NonNull Class<T> type) {
        return getProvider().format(resolve(locale), type);
    }

    default @NonNull String resolve(final @NonNull Locale locale) {
        final String result = getProvider().resolveLiteral(localize(locale));

        return getProvider().getPlaceholderRule()
                .apply(result, getPlaceholders());
    }

    @NonNull Localization localize(@NonNull Locale locale);

    // ==== Object Methods ====================================================

    @Override
    @NonNull String toString();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
