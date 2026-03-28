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

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.exception.*;
import de.leycm.i18label4j.label.LiteralLabel;
import de.leycm.i18label4j.label.LocaleLabel;

import net.lenni0451.mcstructs.text.TextComponent;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.lenni0451.mcstructs.text.components.TranslationComponent;
import net.lenni0451.mcstructs.text.serializer.TextComponentCodec;

import lombok.NonNull;

/**
 * Serializer for converting {@link Label} instances to and from MCStructs {@link TextComponent} objects.
 *
 * <p>This interface extends {@link LabelSerializer} with MCStructs-specific serialization logic.
 * {@link LocaleLabel} instances are serialized as {@link TranslationComponent} objects,
 * while {@link LiteralLabel} instances are converted using the underlying string format
 * defined by the concrete implementation.</p>
 *
 * <p>Concrete implementations are provided as nested classes and wrap MCStructs'
 * {@link TextComponentCodec} to support formats such as JSON (modern), legacy JSON,
 * and plain string.</p>
 *
 * @since 1.0.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface MCStructsTextSerializer extends LiteralFormatSerializer<TextComponent> {

    // ==== MCStructs Interface ===============================================

    /**
     * Serializes the given {@link Label} to an MCStructs {@link TextComponent}.
     *
     * <p>{@link LocaleLabel} instances are converted to a {@link TranslationComponent}
     * using the label's key. {@link LiteralLabel} instances are deserialized
     * from their string literal via {@link #fromLiteral(String)}.</p>
     *
     * @param label the label to serialize; never {@code null}
     * @return the serialized {@link TextComponent}; never {@code null}
     * @throws SerializationException when the label type is unsupported or serialization fails
     */
    @Override
    default @NonNull TextComponent serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof LocaleLabel locale) {
                return new TranslationComponent(locale.getKey());
            } else if (label instanceof LiteralLabel literal) {
                return fromLiteral(literal.getLiteral());
            } else {
                throw new SerializationException("Unsupported label type: " + label.getClass().getName());
            }
        } catch (Exception e) {
            throw new SerializationException(label, e);
        }
    }

    /**
     * Deserializes an MCStructs {@link TextComponent} back into a {@link Label}.
     *
     * <p>{@link TranslationComponent} instances are converted to a locale-aware
     * {@link Label} using the component's translation key. All other component types
     * are converted to a {@link LiteralLabel} via {@link #toLiteral(Object)}.</p>
     *
     * @param serialized the component to deserialize; never {@code null}
     * @param provider   the label provider used to construct the result; never {@code null}
     * @return the deserialized {@link Label}; never {@code null}
     * @throws DeserializationException when deserialization fails
     */
    @Override
    default @NonNull Label deserialize(final @NonNull TextComponent serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        try {
            if (serialized instanceof TranslationComponent translatable) {
                return Label.of(provider, translatable.getKey());
            } else {
                return Label.literal(provider, toLiteral(serialized));
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    // ==== CodecSerializer ===================================================

    /**
     * Base implementation of {@link MCStructsTextSerializer} backed by a MCStructs {@link TextComponentCodec}.
     *
     * <p>Delegates {@link #toLiteral(Object)} and {@link #fromLiteral(String)} to the
     * wrapped {@link TextComponentCodec}, enabling format-agnostic reuse across all
     * MCStructs codec types.</p>
     *
     * @since 1.0.0
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    abstract class CodecSerializer implements MCStructsTextSerializer {

        // wrapped MCStructs codec delegate
        protected final TextComponentCodec codec;

        /**
         * Creates a new {@link CodecSerializer} wrapping the given {@link TextComponentCodec}.
         *
         * @param codec the MCStructs codec to delegate to; never {@code null}
         */
        public CodecSerializer(final @NonNull TextComponentCodec codec) {
            this.codec = codec;
        }
    }

    // ==== CodecSerializer Types =============================================

    /**
     * {@link CodecSerializer} implementation using the latest/modern JSON codec.
     *
     * <p>Uses {@link TextComponentCodec#LATEST} by default. A custom {@link TextComponentCodec}
     * instance can be supplied via the secondary constructor.</p>
     *
     * @since 1.0.0
     * @see CodecSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class MCStructsJson extends CodecSerializer {

        /**
         * Creates a new {@link MCStructsJson} using the latest {@link TextComponentCodec}.
         */
        public MCStructsJson() {
            super(TextComponentCodec.LATEST);
        }

        /**
         * Creates a new {@link MCStructsJson} using the given {@link TextComponentCodec}.
         *
         * @param codec the codec to use; never {@code null}
         */
        public MCStructsJson(final @NonNull TextComponentCodec codec) {
            super(codec);
        }

        /**
         * Serializes the given {@link TextComponent} to a JSON string using the wrapped {@link TextComponentCodec}.
         *
         * @param component the component to serialize; never {@code null}
         * @return the JSON string representation; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull TextComponent component) {
            return codec.serializeJsonString(component);
        }

        /**
         * Deserializes a JSON string to an {@link TextComponent} using the wrapped {@link TextComponentCodec}.
         *
         * @param literal the JSON string to parse; never {@code null}
         * @return the resulting {@link TextComponent}; never {@code null}
         */
        @Override
        public @NonNull TextComponent fromLiteral(@NonNull String literal) {
            return codec.deserializeJson(literal);
        }
    }

    /**
     * {@link CodecSerializer} implementation using the latest/modern JSON codec.
     *
     * <p>Uses {@link TextComponentCodec#LATEST} by default. A custom {@link TextComponentCodec}
     * instance can be supplied via the secondary constructor.</p>
     *
     * @since 1.0.0
     * @see CodecSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class MCStructsNbt extends CodecSerializer {

        /**
         * Creates a new {@link MCStructsJson} using the latest {@link TextComponentCodec}.
         */
        public MCStructsNbt() {
            super(TextComponentCodec.LATEST);
        }

        /**
         * Creates a new {@link MCStructsJson} using the given {@link TextComponentCodec}.
         *
         * @param codec the codec to use; never {@code null}
         */
        public MCStructsNbt(final @NonNull TextComponentCodec codec) {
            super(codec);
        }

        /**
         * Returns the unformatted string content of the given component.
         *
         * @param component the component to convert; never {@code null}
         * @return the plain string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull TextComponent component) {
            return codec.serializeNbtString(component);
        }

        /**
         * Wraps the given string as a {@link StringComponent}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link StringComponent}; never {@code null}
         */
        @Override
        public @NonNull TextComponent fromLiteral(@NonNull String literal) {
            return codec.deserializeNbt(literal);
        }

    }

    /**
     * {@link MCStructsTextSerializer} implementation using plain string (no JSON codec).
     *
     * <p>Wraps raw text directly as a {@link StringComponent} without any codec.
     * {@link #toLiteral(TextComponent)} returns the component's unformatted string content.</p>
     *
     * @since 1.0.0
     * @see MCStructsTextSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class MCStructsPlainText implements MCStructsTextSerializer {

        /**
         * Returns the unformatted string content of the given component.
         *
         * @param component the component to convert; never {@code null}
         * @return the plain string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull TextComponent component) {
            return component.asSingleString();
        }

        /**
         * Wraps the given string as a {@link StringComponent}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link StringComponent}; never {@code null}
         */
        @Override
        public @NonNull TextComponent fromLiteral(@NonNull String literal) {
            return new StringComponent(literal);
        }
    }
}