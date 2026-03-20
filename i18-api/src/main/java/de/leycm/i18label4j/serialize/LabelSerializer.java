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
package de.leycm.i18label4j.serialize;

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.exception.*;

import lombok.NonNull;

/**
 * Strategy interface for converting {@link Label} instances to and from
 * a specific target type {@code T}.
 *
 * <p>A {@link LabelSerializer} is registered against a target class in
 * the {@link LabelProvider} and is invoked by
 * {@link LabelProvider#serialize(Label, Class)},
 * {@link LabelProvider#deserialize(Object)}, and
 * {@link LabelProvider#format(String, Class)}.
 * Typical target types include plain {@link String}, or richer text
 * representations such as Adventure {@code Component} objects.</p>
 *
 * <p>Implementations are expected to be stateless so a single instance
 * may be shared safely across multiple threads.</p>
 *
 * <p>Thread Safety: Implementations must be thread-safe.</p>
 *
 * @param <T> the target serialization type
 *
 * @since 1.0.0
 * @see LabelProvider
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface LabelSerializer<T> {

    /**
     * Converts a {@link Label} into the target type {@code T}.
     *
     * <p>Implementations typically resolve the label's text (e.g. via
     * {@link Label#in()} or {@link Label#mapped()}) and then transform
     * it into the target representation.</p>
     *
     * @param label the label to serialize; must not be {@code null}
     * @return the serialized value; never {@code null}
     * @throws SerializationException if the conversion fails for any
     *                                reason (e.g. unsupported format,
     *                                parse error, downstream exception)
     */
    @NonNull T serialize(@NonNull Label label) throws SerializationException;

    /**
     * Reconstructs a {@link Label} from a previously serialized value.
     *
     * <p>The returned label must behave equivalently to the original
     * label that was passed to {@link #serialize(Label)} for the same
     * value, to the extent possible given the information encoded in
     * the serialized form.</p>
     *
     * @param serialized the serialized representation; must not be {@code null}
     * @return the reconstructed label; never {@code null}
     * @throws DeserializationException if the value cannot be interpreted
     *                                  as a valid label (e.g. corrupt data,
     *                                  unrecognised format)
     */
    @NonNull Label deserialize(@NonNull T serialized) throws DeserializationException;

    /**
     * Formats a raw string directly into the target type {@code T}
     * without going through a full {@link Label} instance.
     *
     * <p>This is useful for lightweight conversions, for example
     * parsing a MiniMessage string into an Adventure {@code Component}
     * without creating an intermediate label object.</p>
     *
     * @param input the raw string to format; must not be {@code null}
     * @return the formatted value; never {@code null}
     * @throws FormatException if the string cannot be converted
     *                         into the target type
     */
    @NonNull T format(@NonNull String input) throws FormatException;
}