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

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * Serializer for converting {@link Label} instances to and from Swing {@link JComponent} objects.
 *
 * <p>This interface extends {@link LiteralFormatSerializer} with Swing-specific serialization
 * logic. {@link LocaleLabel} instances are serialized as {@link JLabel} components whose
 * text is set to the locale key, while {@link LiteralLabel} instances are converted using
 * the underlying string format defined by the concrete implementation.</p>
 *
 * <p>Concrete implementations are provided as nested classes covering the most common
 * Swing text-bearing component types: {@link JLabel}, {@link JTextComponent}
 * (e.g. {@link JTextField}, {@link JTextArea}), and {@link JButton}.</p>
 *
 * @since 1.0.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface SwingComponentSerializer extends LiteralFormatSerializer<JComponent> {

    // ==== Swing Interface ===================================================

    /**
     * Serializes the given {@link Label} to a Swing {@link JComponent}.
     *
     * <p>{@link LocaleLabel} instances are converted to a {@link JLabel}
     * whose text is set to the locale key. {@link LiteralLabel} instances are
     * deserialized from their string literal via {@link #fromLiteral(String)}.</p>
     *
     * @param label the label to serialize; never {@code null}
     * @return the serialized Swing {@link JComponent}; never {@code null}
     * @throws SerializationException when the label type is unsupported or serialization fails
     */
    @Override
    default @NonNull JComponent serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof LocaleLabel locale) {
                return new JLabel(locale.getKey());
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
     * Deserializes a Swing {@link JComponent} back into a {@link Label}.
     *
     * <p>If the component is a {@link JLabel}, it is converted to a locale-aware
     * {@link Label} via {@link Label#of(LabelProvider, String)} using the label's text
     * as the key. All other component types are converted to a {@link LiteralLabel}
     * via {@link #toLiteral(Object)}.</p>
     *
     * @param serialized the component to deserialize; never {@code null}
     * @param provider   the label provider used to construct the result; never {@code null}
     * @return the deserialized {@link Label}; never {@code null}
     * @throws DeserializationException when deserialization fails
     */
    @Override
    default @NonNull Label deserialize(final @NonNull JComponent serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        try {
            if (serialized instanceof JLabel jLabel) {
                return Label.of(provider, jLabel.getText());
            } else {
                return Label.literal(provider, toLiteral(serialized));
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    // ==== Swing Types =======================================================

    /**
     * {@link SwingComponentSerializer} implementation using {@link JLabel} components.
     *
     * <p>Serializes string literals directly as {@link JLabel} instances and reads
     * their content back via {@link JLabel#getText()}.</p>
     *
     * @since 1.0.0
     * @see SwingComponentSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class SwingLabel implements SwingComponentSerializer {

        /**
         * Returns the text content of the given Swing {@link JComponent}.
         *
         * <p>If the component is a {@link JLabel}, its text is returned directly.
         * If the component is a {@link JTextComponent}, its text property is used as fallback.
         * For all other component types, an empty string is returned.</p>
         *
         * @param component the component to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull JComponent component) {
            if (component instanceof JLabel label) return label.getText();
            if (component instanceof JTextComponent text) return text.getText();
            return "";
        }

        /**
         * Wraps the given string as a new {@link JLabel}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link JLabel} with the given text; never {@code null}
         */
        @Override
        public @NonNull JComponent fromLiteral(@NonNull String literal) {
            return new JLabel(literal);
        }
    }

    /**
     * {@link SwingComponentSerializer} implementation using {@link JTextComponent} instances
     * (e.g. {@link JTextField}, {@link JTextArea}).
     *
     * <p>Serializes string literals as {@link JTextField} instances and reads
     * their content back via {@link JTextComponent#getText()}.</p>
     *
     * @since 1.0.0
     * @see SwingComponentSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class SwingTextField implements SwingComponentSerializer {

        /**
         * Returns the text content of the given Swing {@link JComponent}.
         *
         * <p>If the component is a {@link JTextComponent}, its text is returned directly.
         * If the component is a {@link JLabel}, its text property is used as fallback.
         * For all other component types, an empty string is returned.</p>
         *
         * @param component the component to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull JComponent component) {
            if (component instanceof JTextComponent text) return text.getText();
            if (component instanceof JLabel label) return label.getText();
            return "";
        }

        /**
         * Wraps the given string as a new {@link JTextField}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link JTextField} with the given text; never {@code null}
         */
        @Override
        public @NonNull JComponent fromLiteral(@NonNull String literal) {
            return new JTextField(literal);
        }
    }

    /**
     * {@link SwingComponentSerializer} implementation using {@link JButton} components.
     *
     * <p>Serializes string literals as {@link JButton} instances and reads
     * their content back via {@link AbstractButton#getText()}.</p>
     *
     * @since 1.0.0
     * @see SwingComponentSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class SwingButton implements SwingComponentSerializer {

        /**
         * Returns the text content of the given Swing {@link JComponent}.
         *
         * <p>If the component is an {@link AbstractButton}, its text is returned directly.
         * If the component is a {@link JLabel}, its text property is used as fallback.
         * For all other component types, an empty string is returned.</p>
         *
         * @param component the component to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull JComponent component) {
            if (component instanceof AbstractButton button) return button.getText();
            if (component instanceof JLabel label) return label.getText();
            return "";
        }

        /**
         * Wraps the given string as a new {@link JButton}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link JButton} with the given text; never {@code null}
         */
        @Override
        public @NonNull JComponent fromLiteral(@NonNull String literal) {
            return new JButton(literal);
        }
    }
}