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
package de.leycm.label4j.locale;

import lombok.NonNull;

import java.util.Locale;

public final class Locales {
    private static final @NonNull String CONSTANT_FILE_TAG = "const";

    public static @NonNull String localeToFilename(final @NonNull Locale locale) {
        if (locale.equals(Locale.ROOT)) return CONSTANT_FILE_TAG;
        return locale.getLanguage().toLowerCase() + "_" + locale.getCountry().toLowerCase();
    }

    public static @NonNull Locale filenameToLocale(final @NonNull String fileTag) {
        if (fileTag.equalsIgnoreCase(CONSTANT_FILE_TAG)) return Locale.ROOT;
        return Locale.forLanguageTag(fileTag.replace("_", "-"));
    }

    private Locales() {
        throw new UnsupportedOperationException("This util class can not be constructed");
    }
}
