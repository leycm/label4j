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
import de.leycm.i18label4j.serialize.LabelSerializer;
import lombok.NonNull;

/**
 * Thrown when a {@link Label} cannot be converted
 * into the requested target type.
 *
 * <p>This exception is raised by
 * {@link LabelSerializer#serialize(Label)}
 * and propagated through
 * {@link LabelProvider#serialize(Label, Class)}
 * when the label's resolved text is incompatible with the target
 * representation or a downstream library throws during conversion.</p>
 *
 * <p>Thread Safety: Exception instances are not required to be
 * thread-safe; they should not be shared across threads.</p>
 *
 * @since 1.0.0
 * @see LabelProvider#serialize(Label, Class)
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class SerializationException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message; must not be {@code null}
     */
    public SerializationException(final @NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new exception indicating which source value failed
     * and wrapping the root cause.
     *
     * <p>The detail message is formatted as
     * {@code "Value '<source>' could not be serialized:"}.</p>
     *
     * @param source the value that could not be serialized;
     *               must not be {@code null}
     * @param cause  the underlying exception; must not be {@code null}
     */
    public SerializationException(final @NonNull Object source,
                                  final @NonNull Throwable cause) {
        super(String.format("Value '%s' could not be serialized:", source), cause);
    }

    /**
     * Constructs a new exception indicating which source value failed
     * and the human-readable reason.
     *
     * <p>The detail message is formatted as
     * {@code "Value '<source>' could not be serialized: <reason>"}.</p>
     *
     * @param source the value that could not be serialized;
     *               must not be {@code null}
     * @param reason a human-readable explanation; must not be {@code null}
     */
    public SerializationException(final @NonNull Object source,
                                  final @NonNull String reason) {
        super(String.format("Value '%s' could not be serialized: %s", source, reason));
    }
}