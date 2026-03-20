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

import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.serialize.LabelSerializer;
import lombok.NonNull;

/**
 * Thrown when a raw string cannot be converted into the requested
 * target type by a
 * {@link LabelSerializer}.
 *
 * <p>This exception is raised by
 * {@link LabelSerializer#format(String)}
 * and propagated through
 * {@link LabelProvider#format(String, Class)} when
 * the input string is malformed or incompatible with the target type
 * (e.g. an invalid MiniMessage expression when formatting into an
 * Adventure {@code Component}).</p>
 *
 * <p>Thread Safety: Exception instances are not required to be
 * thread-safe; they should not be shared across threads.</p>
 *
 * @since 1.0.0
 * @see LabelProvider#format(String, Class)
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class FormatException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message; must not be {@code null}
     */
    public FormatException(final @NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new exception indicating which source string failed
     * and wrapping the root cause.
     *
     * <p>The detail message is formatted as
     * {@code "Value '<source>' could not be formatted:"}.</p>
     *
     * @param source the string that could not be formatted;
     *               must not be {@code null}
     * @param cause  the underlying exception; must not be {@code null}
     */
    public FormatException(final @NonNull String source,
                           final @NonNull Throwable cause) {
        super(String.format("Value '%s' could not be formated:", source), cause);
    }

    /**
     * Constructs a new exception indicating which source string failed,
     * which target class was attempted, and wrapping the root cause.
     *
     * <p>The detail message is formatted as
     * {@code "Value '<source>' could not be formatted into <class>:"}.</p>
     *
     * @param source the string that could not be formatted;
     *               must not be {@code null}
     * @param clazz  the target class that was attempted;
     *               must not be {@code null}
     * @param cause  the underlying exception; must not be {@code null}
     */
    public FormatException(final @NonNull String source,
                           final @NonNull Class<?> clazz,
                           final @NonNull Throwable cause) {
        super(String.format("Value '%s' could not be formated into %s:", source, clazz.getName()), cause);
    }
}