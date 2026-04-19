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
package de.leycm.label4j.exception;

import lombok.NonNull;

public class IncompatibleMatchException extends ClassCastException {

    public IncompatibleMatchException(final @NonNull String message) {
        super(message);
    }

    public IncompatibleMatchException(final @NonNull Class<?> type) {
        super("Serializer for type " + type.getName() + " returned incompatible type");
    }

    public IncompatibleMatchException(final @NonNull Class<?> type,
                                      final @NonNull Throwable cause) {
        super(String.format("Serializer for type " + type.getName()
                + " returned incompatible type: %s", cause.getMessage()));
    }
}