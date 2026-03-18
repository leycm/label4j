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
package de.leycm.i18label4j.source;

import lombok.NonNull;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public interface LocalizationSource extends Iterable<Locale> {

    @NonNull Set<Locale> getLocalizations();

    @NonNull Map<String, String> getLocalization(@NonNull Locale locale) throws Exception;

    // note: logic can and should in some cases be replaced by a more efficient implementation
    default boolean containsLocalization(@NonNull Locale locale) {
        return getLocalizations().contains(locale);
    }

}
