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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import lombok.NonNull;

/**
 * Serializer for converting {@link Label} instances to and from Adventure {@link Component} objects.
 *
 * <p>This interface extends {@link LabelSerializer} with Adventure-specific serialization logic.
 * {@link LocaleLabel} instances are serialized as {@link TranslatableComponent} objects,
 * while {@link LiteralLabel} instances are converted using the underlying string format
 * defined by the concrete implementation.</p>
 *
 * <p>Concrete implementations are provided as nested classes and wrap Kyori's
 * {@link ComponentSerializer} to support formats such as MiniMessage, legacy section
 * sign formatting, JSON, and plain text.</p>
 *
 * @since 1.0.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface AdventureComponentSerializer extends LabelSerializer<Component> {

    // ==== Adventure Interface ===============================================

    /**
     * Serializes the given {@link Label} to an Adventure {@link Component}.
     *
     * <p>{@link LocaleLabel} instances are converted to a {@link TranslatableComponent}
     * using the label's key and fallback. {@link LiteralLabel} instances are deserialized
     * from their string literal via {@link #fromLiteral(String)}.</p>
     *
     * @param label the label to serialize; never {@code null}
     * @return the serialized {@link Component}; never {@code null}
     * @throws SerializationException when the label type is unsupported or serialization fails
     */
    @Override
    default @NonNull Component serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof LocaleLabel locale) {
                return Component.translatable(locale.getKey(), locale.getFallback());
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
     * Deserializes an Adventure {@link Component} back into a {@link Label}.
     *
     * <p>{@link TranslatableComponent} instances are converted to a locale-aware
     * {@link Label} using the component's key and fallback. All other component types
     * are converted to a {@link LiteralLabel} via {@link #toLiteral(Component)}.</p>
     *
     * @param serialized the component to deserialize; never {@code null}
     * @param provider   the label provider used to construct the result; never {@code null}
     * @return the deserialized {@link Label}; never {@code null}
     * @throws DeserializationException when deserialization fails
     */
    @Override
    default @NonNull Label deserialize(final @NonNull Component serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        try {
            if (serialized instanceof TranslatableComponent translatable) {
                return Label.of(provider, translatable.key(), translatable.fallback());
            } else {
                return Label.literal(provider, toLiteral(serialized));
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    /**
     * Parses a raw string input into an Adventure {@link Component}.
     *
     * @param input the string to parse; never {@code null}
     * @return the resulting {@link Component}; never {@code null}
     * @throws FormatException when the input cannot be parsed
     */
    @Override
    default @NonNull Component format(@NonNull String input) throws FormatException {
        try {
            return fromLiteral(input);
        } catch (Exception e) {
            throw new FormatException(input, e);
        }
    }

    /**
     * Converts an Adventure {@link Component} to its string literal representation.
     *
     * @param component the component to convert; never {@code null}
     * @return the string literal; never {@code null}
     */
    @NonNull String toLiteral(@NonNull Component component);

    /**
     * Converts a string literal to an Adventure {@link Component}.
     *
     * @param literal the string to parse; never {@code null}
     * @return the resulting {@link Component}; never {@code null}
     */
    @NonNull Component fromLiteral(@NonNull String literal);

    // ==== KyoriSerializer ===================================================

    /**
     * Base implementation of {@link AdventureComponentSerializer} backed by a Kyori {@link ComponentSerializer}.
     *
     * <p>Delegates {@link #toLiteral(Component)} and {@link #fromLiteral(String)} to the
     * wrapped {@link ComponentSerializer}, enabling format-agnostic reuse across all
     * Kyori serializer types.</p>
     *
     * @since 1.0.0
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class KyoriSerializer implements AdventureComponentSerializer {

        // wrapped Kyori serializer delegate
        protected final ComponentSerializer<Component, ?, String> serializer;

        /**
         * Creates a new {@link KyoriSerializer} wrapping the given {@link ComponentSerializer}.
         *
         * @param serializer the Kyori serializer to delegate to; never {@code null}
         */
        public KyoriSerializer(final @NonNull ComponentSerializer<Component, ?, String> serializer) {
            this.serializer = serializer;
        }

        /**
         * Serializes the given {@link Component} to a string using the wrapped {@link ComponentSerializer}.
         *
         * @param component the component to serialize; never {@code null}
         * @return the string representation; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            return serializer.serialize(component);
        }

        /**
         * Deserializes a string to a {@link Component} using the wrapped {@link ComponentSerializer}.
         *
         * @param literal the string to parse; never {@code null}
         * @return the resulting {@link Component}; never {@code null}
         */
        @Override
        public @NonNull Component fromLiteral(@NonNull String literal) {
            return serializer.deserialize(literal);
        }
    }

    // ==== KyoriSerializer Types =============================================

    /**
     * {@link KyoriSerializer} implementation using MiniMessage formatting.
     *
     * <p>Uses {@link MiniMessage#miniMessage()} by default. A custom {@link MiniMessage}
     * instance can be supplied via the secondary constructor.</p>
     *
     * @since 1.0.0
     * @see KyoriSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class KyoriMiniMessage extends KyoriSerializer {

        /**
         * Creates a new {@link KyoriMiniMessage} using the default {@link MiniMessage} instance.
         */
        public KyoriMiniMessage() {
            super(MiniMessage.miniMessage());
        }

        /**
         * Creates a new {@link KyoriMiniMessage} using the given {@link MiniMessage} instance.
         *
         * @param miniMessage the MiniMessage instance to use; never {@code null}
         */
        public KyoriMiniMessage(final @NonNull MiniMessage miniMessage) {
            super(miniMessage);
        }
    }

    /**
     * {@link KyoriSerializer} implementation using legacy section sign ({@code §}) formatting.
     *
     * <p>Uses {@link LegacyComponentSerializer#legacySection()} by default. A custom
     * {@link LegacyComponentSerializer} instance can be supplied via the secondary constructor.</p>
     *
     * @since 1.0.0
     * @see KyoriSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class KyoriLegacy extends KyoriSerializer {

        /**
         * Creates a new {@link KyoriLegacy} using the default legacy section serializer.
         */
        public KyoriLegacy() {
            super(LegacyComponentSerializer.legacySection());
        }

        /**
         * Creates a new {@link KyoriLegacy} using the default legacy section serializer.
         *
         * <p>Note: the provided {@code serializer} parameter is currently ignored;
         * {@link LegacyComponentSerializer#legacySection()} is always used.</p>
         *
         * @param serializer unused; never {@code null}
         */
        public KyoriLegacy(final @NonNull LegacyComponentSerializer serializer) {
            super(LegacyComponentSerializer.legacySection());
        }
    }

    /**
     * {@link KyoriSerializer} implementation using JSON formatting.
     *
     * <p>Uses {@link JSONComponentSerializer#json()} by default. A custom
     * {@link JSONComponentSerializer} instance can be supplied via the secondary constructor.</p>
     *
     * @since 1.0.0
     * @see KyoriSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class KyoriJson extends KyoriSerializer {

        /**
         * Creates a new {@link KyoriJson} using the default JSON serializer.
         */
        public KyoriJson() {
            super(JSONComponentSerializer.json());
        }

        /**
         * Creates a new {@link KyoriJson} using the given {@link JSONComponentSerializer}.
         *
         * @param serializer the JSON serializer to use; never {@code null}
         */
        public KyoriJson(final @NonNull JSONComponentSerializer serializer) {
            super(serializer);
        }
    }

    /**
     * {@link KyoriSerializer} implementation using plain text formatting.
     *
     * <p>Uses {@link PlainTextComponentSerializer#plainText()} by default. A custom
     * {@link PlainTextComponentSerializer} instance can be supplied via the secondary constructor.</p>
     *
     * @since 1.0.0
     * @see KyoriSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class KyoriPlainText extends KyoriSerializer {

        /**
         * Creates a new {@link KyoriPlainText} using the default plain text serializer.
         */
        public KyoriPlainText() {
            super(PlainTextComponentSerializer.plainText());
        }

        /**
         * Creates a new {@link KyoriPlainText} using the given {@link PlainTextComponentSerializer}.
         *
         * @param serializer the plain text serializer to use; never {@code null}
         */
        public KyoriPlainText(final @NonNull PlainTextComponentSerializer serializer) {
            super(serializer);
        }
    }
}