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

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable record wrapping the outcome of a single translation lookup.
 *
 * <p>A {@link LocalizedResult} holds either a translated string value
 * or {@code null} when no translation was found for the requested key
 * and locale. The {@link #or(String)} method provides a convenient
 * way to unwrap the result with a fallback value in one step.</p>
 *
 * <p>Instances of this record are stored in the translation cache of
 * {@link CommonLabelProvider} and are therefore shared across threads.
 * Being a record with only immutable fields, this class is inherently
 * thread-safe.</p>
 *
 * @param localized the translated string, or {@code null} when no
 *                  translation was found
 *
 * @since 1.0
 * @see CommonLabelProvider
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public record LocalizedResult(@Nullable String localized) {

    /**
     * Returns the translated string if present, otherwise returns
     * {@code defaultValue}.
     *
     * @param defaultValue the fallback value used when {@link #localized()}
     *                     is {@code null}; must not be {@code null}
     * @return the translated string or {@code defaultValue};
     *         never {@code null}
     */
    public @NonNull String or(final @NonNull String defaultValue) {
        return localized != null ? localized : defaultValue;
    }

    /**
     * Returns the translated string value.
     *
     * @return the translated string, or {@code null} when no translation
     *         was found for the requested key and locale
     */
    @Override
    public @Nullable String localized() {
        return localized;
    }
}