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
package de.leycm.i18label4j.serializer;

import de.leycm.i18label4j.exception.FormatException;

import lombok.NonNull;

/**
 * Extension of {@link LabelSerializer} for serializers that support
 * formatting raw strings into the target type.
 *
 * <p>Implementations must provide a {@code fromLiteral} method that
 * converts a raw string into the target type, and a {@code toLiteral}
 * method that converts an instance of the target type back into a string.</p>
 *
 * <p>The default implementation of {@code format} delegates to
 * {@code fromLiteral} and wraps any exceptions in a {@link FormatException}
 * with a message that identifies the input string.</p>
 *
 * <p>Thread Safety: Implementations must be thread-safe.</p>
 *
 * @param <T> the target serialization type
 *
 * @since 1.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface LiteralFormatSerializer<T> extends LabelSerializer<T> {

    /**
     * Converts a raw string into the target type {@code T}.
     *
     * <p>Implementations must provide a concrete implementation of this
     * method that performs the actual parsing and conversion logic.</p>
     *
     * @param input the raw string to format; must not be {@code null}
     * @return the formatted value; never {@code null}
     * @throws FormatException if the conversion fails for any reason
     *                         (e.g. malformed input, unsupported format,
     *                         parse error, downstream exception)
     */
    @Override
    default @NonNull T format(@NonNull String input) throws FormatException {
        try {
            return fromLiteral(input);
        } catch (Exception e) {
            throw new FormatException(input, e);
        }
    }

    /**
     * Converts an instance of the target type {@code T} back into a raw
     * string.
     *
     * <p>Implementations must provide a concrete implementation of this
     * method that performs the actual serialization logic.</p>
     *
     * @param component the value to convert; must not be {@code null}
     * @return the literal string representation; never {@code null}
     * @throws Exception if the conversion fails for any reason
     *                   (e.g. unsupported format, downstream exception)
     */
    @NonNull String toLiteral(@NonNull T component) throws Exception;

    /**
     * Converts a raw string into an instance of the target type {@code T}.
     *
     * <p>Implementations must provide a concrete implementation of this
     * method that performs the actual parsing and conversion logic.</p>
     *
     * @param literal the raw string to convert; must not be {@code null}
     * @return the converted value; never {@code null}
     * @throws Exception if the conversion fails for any reason
     *                   (e.g. malformed input, unsupported format,
     *                   parse error, downstream exception)
     */
    @NonNull T fromLiteral(@NonNull String literal) throws Exception;

}
