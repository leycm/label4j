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
package de.leycm.label4j.excaption;

import lombok.NonNull;

public class FormatException extends RuntimeException {

    public FormatException(final @NonNull String message) {
        super(message);
    }

    public FormatException(final @NonNull String source,
                           final @NonNull Throwable cause) {
        super(String.format("Value '%s' could not be formatted:", source), cause);
    }

    public FormatException(final @NonNull String source,
                           final @NonNull Class<?> clazz,
                           final @NonNull Throwable cause) {
        super(String.format("Value '%s' could not be formatted into %s:", source, clazz.getName()), cause);
    }
}