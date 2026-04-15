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

import de.leycm.label4j.exception.DuplicatePlaceholderException;
import de.leycm.label4j.localization.Localization;
import de.leycm.label4j.placeholder.Placeholder;

import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public interface Label {

    // ==== Field Methods =====================================================

    @NonNull LabelProvider getProvider();

    @UnmodifiableView
    @NonNull Set<Placeholder> getPlaceholders();

    // ==== Replacement Methods ===============================================

    default @NonNull Label replace(
            final @NonNull String key,
            final @NonNull Object value
    ) throws DuplicatePlaceholderException {
        return replace(key, () -> value);
    }

    default @NonNull Label replace(
            final @NonNull String key,
            final @NonNull Supplier<Object> supplier
    ) throws DuplicatePlaceholderException {
        return replace(new Placeholder(key, supplier));
    }

    @NonNull Label replace(@NonNull Placeholder mapping)
            throws DuplicatePlaceholderException;

    // ==== Resolution Methods ================================================
    // todo: add fallback handling to LabelProvider

    default @NonNull <T> T resolveDefault(final @NonNull Class<T> type) {
        return getProvider().format(resolveDefault(), type);
    }

    default @NonNull String resolveDefault() {
        final Localization localization = localizeDefault();
        return getProvider().getPlaceholderRule()
                .apply(localization.orElse(""),
                        getPlaceholders());
    }

    default @NonNull <T> T resolve(
            final @NonNull Locale locale,
            final @NonNull Class<T> type) {
        return getProvider().format(resolve(locale), type);
    }

    default @NonNull String resolve(final  @NonNull Locale locale) {
        final Localization localization = localize(locale);
        return getProvider().getPlaceholderRule()
                .apply(localization.orElse(""), getPlaceholders());
    }

    @NonNull Localization localizeDefault();

    @NonNull Localization localize(@NonNull Locale locale);

    // ==== Object Methods ====================================================

    @Override
    @NonNull String toString();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
