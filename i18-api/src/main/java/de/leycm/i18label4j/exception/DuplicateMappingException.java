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

/**
 * Thrown when an illegal argument specific to the i18label4j domain
 * is provided to an API method.
 *
 * <p>This exception is typically used instead of {@link IllegalArgumentException}
 * when a more domain-specific error type is desirable, such as when a mapping
 * with a duplicate key is registered.</p>
 *
 * <p>Thread Safety: Exception instances are not required to be thread-safe;
 * they should not be shared across threads.</p>
 *
 * @since 1.0
 */
public class DuplicateMappingException extends IllegalArgumentException {

    /**
     * Constructs a new exception for the given duplicate key.
     *
     * @param key the duplicate key; never {@code null}
     */
    public DuplicateMappingException(final @NonNull Object key) {
        super(String.format("Mapping with key \"%s\" already exists for this LabelConstructor.", key));
    }

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message; never {@code null}
     */
    public DuplicateMappingException(final @NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given detail message
     * and underlying cause.
     *
     * @param message the detail message; never {@code null}
     * @param cause   the underlying cause; never {@code null}
     */
    public DuplicateMappingException(final @NonNull String message,
                                     final @NonNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the given cause.
     *
     * <p>The detail message will be derived from the cause.</p>
     *
     * @param cause the underlying cause; never {@code null}
     */
    public DuplicateMappingException(final @NonNull Throwable cause) {
        super(cause);
    }
}