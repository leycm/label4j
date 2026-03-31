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

import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import lombok.NonNull;

/**
 * Serializer for converting {@link Label} instances to and from JavaFX {@link Node} objects.
 *
 * <p>This interface extends {@link LiteralFormatSerializer} with JavaFX-specific serialization
 * logic. {@link LocaleLabel} instances are serialized as JavaFX {@link Text} nodes whose
 * text is set to the locale key, while {@link LiteralLabel} instances are converted using
 * the underlying string format defined by the concrete implementation.</p>
 *
 * <p>Concrete implementations are provided as nested classes covering the most common
 * JavaFX text-bearing node types: {@link Text}, {@link Labeled} (e.g. {@link javafx.scene.control.Label},
 * {@link javafx.scene.control.Button}), and {@link TextFlow}.</p>
 *
 * @since 1.0.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface JavaFxSerializer extends LiteralFormatSerializer<Node> {

    // ==== JavaFX Interface ==================================================

    /**
     * Serializes the given {@link Label} to a JavaFX {@link Node}.
     *
     * <p>{@link LocaleLabel} instances are converted to a {@link Text} node
     * whose text is set to the locale key. {@link LiteralLabel} instances are
     * deserialized from their string literal via {@link #fromLiteral(String)}.</p>
     *
     * @param label the label to serialize; never {@code null}
     * @return the serialized JavaFX {@link Node}; never {@code null}
     * @throws SerializationException when the label type is unsupported or serialization fails
     */
    @Override
    default @NonNull Node serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof final LocaleLabel locale) {
                return new Text(locale.getKey());
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
     * Deserializes a JavaFX {@link Node} back into a {@link Label}.
     *
     * <p>If the node is a {@link Text} node, it is converted to a locale-aware
     * {@link Label} via {@link Label#of(LabelProvider, String)} using the node's text
     * content as the key. All other node types are converted to a {@link LiteralLabel}
     * via {@link #toLiteral(Object)}.</p>
     *
     * @param serialized the node to deserialize; never {@code null}
     * @param provider   the label provider used to construct the result; never {@code null}
     * @return the deserialized {@link Label}; never {@code null}
     * @throws DeserializationException when deserialization fails
     */
    @Override
    default @NonNull Label deserialize(final @NonNull Node serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        try {
            if (serialized instanceof final Text text) {
                return Label.of(provider, text.getText());
            } else {
                return Label.literal(provider, toLiteral(serialized));
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    // ==== JavaFX Types ======================================================

    /**
     * {@link JavaFxSerializer} implementation using JavaFX {@link Text} nodes.
     *
     * <p>Serializes string literals directly as {@link Text} instances and reads
     * their content back via {@link Text#getText()}.</p>
     *
     * @since 1.0.0
     * @see JavaFxSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class FxText implements JavaFxSerializer {

        /**
         * Returns the text content of the given JavaFX {@link Node}.
         *
         * <p>If the node is a {@link Text}, its text is returned directly.
         * If the node is a {@link Labeled} control, its text property is used.
         * For all other node types, an empty string is returned.</p>
         *
         * @param node the node to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull Node node) {
            if (node instanceof final Text text) return text.getText();
            if (node instanceof final Labeled labeled) return labeled.getText();
            return "";
        }

        /**
         * Wraps the given string as a new JavaFX {@link Text} node.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link Text} node with the given content; never {@code null}
         */
        @Override
        public @NonNull Node fromLiteral(@NonNull String literal) {
            return new Text(literal);
        }
    }

    /**
     * {@link JavaFxSerializer} implementation using JavaFX {@link Labeled} controls
     * (e.g. {@link javafx.scene.control.Label}, {@link javafx.scene.control.Button}).
     *
     * <p>Serializes string literals as {@link javafx.scene.control.Label} instances and reads
     * their content back via {@link Labeled#getText()}.</p>
     *
     * @since 1.0.0
     * @see JavaFxSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class FxLabeled implements JavaFxSerializer {

        /**
         * Returns the text content of the given JavaFX {@link Node}.
         *
         * <p>If the node is a {@link Labeled} control, its text is returned directly.
         * If the node is a {@link Text} node, its text property is used as fallback.
         * For all other node types, an empty string is returned.</p>
         *
         * @param node the node to convert; never {@code null}
         * @return the string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull Node node) {
            if (node instanceof final Labeled labeled) return labeled.getText();
            if (node instanceof final Text text) return text.getText();
            return "";
        }

        /**
         * Wraps the given string as a new {@link javafx.scene.control.Label}.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link javafx.scene.control.Label} with the given text; never {@code null}
         */
        @Override
        public @NonNull Node fromLiteral(@NonNull String literal) {
            return new javafx.scene.control.Label(literal);
        }
    }

    /**
     * {@link JavaFxSerializer} implementation using JavaFX {@link TextFlow} nodes.
     *
     * <p>Serializes string literals as a {@link TextFlow} containing a single {@link Text}
     * child node. Reads back the concatenated text of all {@link Text} children.</p>
     *
     * @since 1.0.0
     * @see JavaFxSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class FxTextFlow implements JavaFxSerializer {

        /**
         * Returns the concatenated text content of all {@link Text} children in the given {@link Node}.
         *
         * <p>If the node is a {@link TextFlow}, all {@link Text} child nodes are concatenated
         * in order. If the node is itself a {@link Text} node, its text is returned directly.
         * For all other node types, an empty string is returned.</p>
         *
         * @param node the node to convert; never {@code null}
         * @return the concatenated string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull Node node) {
            if (node instanceof final TextFlow flow) {
                final StringBuilder sb = new StringBuilder();
                flow.getChildren().forEach(child -> {
                    if (child instanceof final Text text) sb.append(text.getText());
                });
                return sb.toString();
            }
            if (node instanceof final Text text) return text.getText();
            return "";
        }

        /**
         * Wraps the given string as a {@link TextFlow} containing a single {@link Text} child.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link TextFlow} with a single {@link Text} child; never {@code null}
         */
        @Override
        public @NonNull Node fromLiteral(@NonNull String literal) {
            return new TextFlow(new Text(literal));
        }
    }
}
