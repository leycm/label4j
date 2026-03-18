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
package de.leycm.i18label4j.exception;

import lombok.NonNull;

public class IncompatibleMatchException extends ClassCastException {

    public IncompatibleMatchException(final @NonNull Class<?> type) {
        super("Serializer for type " + type.getName() + " returned incompatible type");
    }

    public IncompatibleMatchException(final @NonNull Class<?> type, final @NonNull Throwable cause) {
        super(String.format("Serializer for type " + type.getName() + " returned incompatible type: %f", cause.getMessage()));
    }

}