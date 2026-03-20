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
 * Thrown when a registered {@link LabelSerializer}
 * returns a value whose runtime type is incompatible with the requested
 * target type.
 *
 * <p>This exception extends {@link ClassCastException} and is raised by
 * {@code CommonLabelProvider} when an unchecked cast
 * of a serializer's return value fails. It replaces the raw
 * {@link ClassCastException} with a message that identifies the target
 * type, making diagnostics easier.</p>
 *
 * <p>Thread Safety: Exception instances are not required to be
 * thread-safe; they should not be shared across threads.</p>
 *
 * @since 1.0.0
 * @see LabelProvider#serialize(Label, Class)
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class IncompatibleMatchException extends ClassCastException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message; must not be {@code null}
     */
    public IncompatibleMatchException(final @NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new exception identifying which target type caused
     * the incompatibility.
     *
     * <p>The detail message is formatted as
     * {@code "Serializer for type <type> returned incompatible type"}.</p>
     *
     * @param type the requested target type that could not be satisfied;
     *             must not be {@code null}
     */
    public IncompatibleMatchException(final @NonNull Class<?> type) {
        super("Serializer for type " + type.getName() + " returned incompatible type");
    }

    /**
     * Constructs a new exception identifying which target type caused
     * the incompatibility and including the original cause's message.
     *
     * <p>The detail message is formatted as
     * {@code "Serializer for type <type> returned incompatible type: <cause message>"}.</p>
     *
     * @param type  the requested target type that could not be satisfied;
     *              must not be {@code null}
     * @param cause the underlying {@link ClassCastException};
     *              must not be {@code null}
     */
    public IncompatibleMatchException(final @NonNull Class<?> type,
                                      final @NonNull Throwable cause) {
        super(String.format("Serializer for type " + type.getName()
                + " returned incompatible type: %s", cause.getMessage()));
    }
}