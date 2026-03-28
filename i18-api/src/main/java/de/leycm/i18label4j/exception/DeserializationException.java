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

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.serializer.LabelSerializer;

import lombok.NonNull;

/**
 * Thrown when a serialized value cannot be converted back into a
 * {@link Label}.
 *
 * <p>This exception is raised by
 * {@link LabelSerializer#deserialize(Object, LabelProvider)}
 * and propagated through
 * {@link LabelProvider#deserialize(Object)} when the
 * input data is corrupt, has an unrecognized format, or cannot be mapped
 * to a valid label for any other reason.</p>
 *
 * <p>Thread Safety: Exception instances are not required to be
 * thread-safe; they should not be shared across threads.</p>
 *
 * @since 1.0
 * @see LabelProvider#deserialize(Object)
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class DeserializationException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message; never {@code null}
     */
    public DeserializationException(final @NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new exception indicating which source value failed
     * and wrapping the root cause.
     *
     * <p>The detail message is formatted as
     * {@code "Value '<source>' could not be deserialized:"}.</p>
     *
     * @param source the value that could not be deserialized;
     *               never {@code null}
     * @param cause  the underlying exception; never {@code null}
     */
    public DeserializationException(final @NonNull Object source,
                                    final @NonNull Throwable cause) {
        super(String.format("Value '%s' could not be deserialized:", source), cause);
    }

    /**
     * Constructs a new exception indicating which source value failed
     * and the human-readable reason.
     *
     * <p>The detail message is formatted as
     * {@code "Value '<source>' could not be deserialized: <reason>"}.</p>
     *
     * @param source the value that could not be deserialized;
     *               never {@code null}
     * @param reason a human-readable explanation; never {@code null}
     */
    public DeserializationException(final @NonNull Object source,
                                    final @NonNull String reason) {
        super(String.format("Value '%s' could not be deserialized: %s", source, reason));
    }
}