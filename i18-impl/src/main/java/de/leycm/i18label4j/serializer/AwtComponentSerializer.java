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

import lombok.NonNull;

import java.awt.Component;
import java.awt.TextComponent;

/**
 * Serializer for converting {@link Label} instances to and from AWT
 * {@link Component} objects.
 *
 * <p>This interface extends {@link LiteralFormatSerializer} with AWT-specific serialization
 * logic. {@link LocaleLabel} instances are serialized as AWT {@link Label} components
 * whose text is the locale key, while {@link LiteralLabel} instances are converted using
 * the underlying string format defined by the concrete implementation.</p>
 *
 * <p>Concrete implementations are provided as nested classes and cover the two most
 * common AWT text-bearing component types: plain {@link Label} and editable
 * {@link TextComponent} (e.g. {@link java.awt.TextField}).</p>
 *
 * @since 1.0.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface AwtComponentSerializer extends LiteralFormatSerializer<Component> {

    // ==== AWT Interface =====================================================

    /**
     * Serializes the given {@link Label} to an AWT {@link Component}.
     *
     * <p>{@link LocaleLabel} instances are converted to an AWT {@link Label}
     * whose text is set to the locale key. {@link LiteralLabel} instances are
     * deserialized from their string literal via {@link #fromLiteral(String)}.</p>
     *
     * @param label the label to serialize; never {@code null}
     * @return the serialized AWT {@link Component}; never {@code null}
     * @throws SerializationException when the label type is unsupported or serialization fails
     */
    @Override
    default @NonNull Component serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof final LocaleLabel locale) {
                return new java.awt.Label(locale.getKey());
            } else if (label instanceof final LiteralLabel literal) {
                return fromLiteral(literal.getLiteral());
            } else {
                throw new SerializationException("Unsupported label type: " + label.getClass().getName());
            }
        } catch (Exception e) {
            throw new SerializationException(label, e);
        }
    }

    /**
     * Deserializes an AWT {@link Component} back into a {@link Label}.
     *
     * <p>If the component is an AWT {@link Label} whose text matches a known locale key,
     * it is converted to a locale-aware {@link Label} via
     * {@link Label#of(LabelProvider, String)}. All other component
     * types are converted to a {@link LiteralLabel} via {@link #toLiteral(Object)}.</p>
     *
     * @param serialized the component to deserialize; never {@code null}
     * @param provider   the label provider used to construct the result; never {@code null}
     * @return the deserialized {@link Label}; never {@code null}
     * @throws DeserializationException when deserialization fails
     */
    @Override
    default @NonNull Label deserialize(final @NonNull Component serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        try {
            if (serialized instanceof final java.awt.Label awtLabel) {
                return Label.of(provider, awtLabel.getText());
            } else {
                return Label.literal(provider, toLiteral(serialized));
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    // ==== AWT Types =========================================================

    /**
     * {@link AwtComponentSerializer} implementation using AWT {@link Label} components.
     *
     * <p>Serializes string literals directly as AWT {@link Label} instances and
     * reads their text back via {@link java.awt.Label#getText()}.</p>
     *
     * @since 1.0.0
     * @see AwtComponentSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class AwtLabel implements AwtComponentSerializer {

        /**
         * Returns the text content of the given AWT {@link Component}.
         *
         * <p>If the component is an AWT {@link Label}, its text is returned directly.
         * For all other component types, {@link Component#getName()} is used as fallback.</p>
         *
         * @param component the component to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            if (component instanceof final java.awt.Label label) {
                return label.getText();
            }
            return component.getName();
        }

        /**
         * Wraps the given string as a new AWT {@link Label}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link Label} with the given text; never {@code null}
         */
        @Override
        public @NonNull Component fromLiteral(@NonNull String literal) {
            return new java.awt.Label(literal);
        }
    }

    /**
     * {@link AwtComponentSerializer} implementation using AWT {@link TextComponent} instances
     * (e.g. {@link java.awt.TextField} or {@link java.awt.TextArea}).
     *
     * <p>Serializes string literals as {@link java.awt.TextField} instances and reads
     * their text back via {@link TextComponent#getText()}.</p>
     *
     * @since 1.0.0
     * @see AwtComponentSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class AwtTextField implements AwtComponentSerializer {

        /**
         * Returns the text content of the given AWT {@link Component}.
         *
         * <p>If the component is an AWT {@link TextComponent}, its text is returned directly.
         * For all other component types, {@link Component#getName()} is used as fallback.</p>
         *
         * @param component the component to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            if (component instanceof final TextComponent textComponent) {
                return textComponent.getText();
            }
            return component.getName();
        }

        /**
         * Wraps the given string as a new {@link java.awt.TextField}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link java.awt.TextField} with the given text; never {@code null}
         */
        @Override
        public @NonNull Component fromLiteral(@NonNull String literal) {
            return new java.awt.TextField(literal);
        }
    }
}