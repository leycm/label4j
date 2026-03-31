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

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import lombok.NonNull;

/**
 * Serializer for converting {@link Label} instances to and from BungeeCord {@link BaseComponent} arrays.
 *
 * <p>This interface extends {@link LabelSerializer} with BungeeCord-specific serialization logic.
 * {@link LocaleLabel} instances are serialized as {@link TranslatableComponent} objects,
 * while {@link LiteralLabel} instances are converted using the underlying string format
 * defined by the concrete implementation.</p>
 *
 * <p>Concrete implementations are provided as nested classes and wrap BungeeCord's
 * {@link ComponentSerializer} to support formats such as JSON and plain text.</p>
 *
 * @since 1.0.0
 * @see LabelSerializer
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface BungeeChatSerializer extends LiteralFormatSerializer<BaseComponent> {

    // ==== BungeeCord Interface ==============================================

    /**
     * Serializes the given {@link Label} to a BungeeCord {@link BaseComponent} array.
     *
     * <p>{@link LocaleLabel} instances are converted to a {@link TranslatableComponent}
     * using the label's key. {@link LiteralLabel} instances are deserialized
     * from their string literal via {@link #fromLiteral(String)}.</p>
     *
     * @param label the label to serialize; never {@code null}
     * @return the serialized {@link BaseComponent} array; never {@code null}
     * @throws SerializationException when the label type is unsupported or serialization fails
     */
    @Override
    default @NonNull BaseComponent serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof final LocaleLabel locale) {
                return new TranslatableComponent(locale.getKey());
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
     * Deserializes a BungeeCord {@link BaseComponent} array back into a {@link Label}.
     *
     * <p>If the array contains exactly one element that is a {@link TranslatableComponent},
     * it is converted to a locale-aware {@link Label} using the component's translation key.
     * All other cases are converted to a {@link LiteralLabel} via {@link #toLiteral(Object)}.</p>
     *
     * @param serialized the component array to deserialize; never {@code null}
     * @param provider   the label provider used to construct the result; never {@code null}
     * @return the deserialized {@link Label}; never {@code null}
     * @throws DeserializationException when deserialization fails
     */
    @Override
    default @NonNull Label deserialize(final @NonNull BaseComponent serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        try {
            if (serialized instanceof final TranslatableComponent translatable) {
                return Label.of(provider, translatable.getTranslate());
            } else {
                return Label.literal(provider, toLiteral(serialized));
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    // ==== BungeeChat Types ==================================================

    /**
     * {@link BungeeChatSerializer} implementation using JSON formatting.
     *
     * <p>Delegates to {@link ComponentSerializer#toString(BaseComponent[])} and
     * {@link ComponentSerializer#parse(String)} for serialization and deserialization.</p>
     *
     * @since 1.0.0
     * @see BungeeChatSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class BungeeJson implements BungeeChatSerializer {

        /**
         * Serializes the given {@link BaseComponent} array to a JSON string.
         *
         * @param component the component to serialize; never {@code null}
         * @return the JSON string representation; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull BaseComponent component) {
            return ComponentSerializer.toString(component);
        }

        /**
         * Deserializes a JSON string to a {@link BaseComponent} array.
         *
         * @param literal the JSON string to parse; never {@code null}
         * @return the resulting {@link BaseComponent} array; never {@code null}
         */
        @Override
        public @NonNull BaseComponent fromLiteral(@NonNull String literal) {
            return ComponentSerializer.deserialize(literal);
        }
    }

    /**
     * {@link BungeeChatSerializer} implementation using plain text formatting.
     *
     * <p>Wraps raw text directly as a {@link TextComponent} without any JSON codec.
     * {@link #toLiteral(BaseComponent)} concatenates the plain text of all component.</p>
     *
     * @since 1.0.0
     * @see BungeeChatSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class BungeePlainText implements BungeeChatSerializer {

        /**
         * Returns the concatenated plain text content of all given component.
         *
         * @param component the component to convert; never {@code null}
         * @return the plain string content; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull BaseComponent component) {
            return component.toPlainText();
        }

        /**
         * Wraps the given string as a single-element {@link TextComponent} array.
         *
         * @param literal the string to wrap; never {@code null}
         * @return a new {@link BaseComponent} array containing a single {@link TextComponent}; never {@code null}
         */
        @Override
        public @NonNull BaseComponent fromLiteral(@NonNull String literal) {
            return new TextComponent(literal);
        }
    }

    /**
     * {@link BungeeChatSerializer} implementation using legacy section sign ({@code §}) formatting.
     *
     * <p>Uses {@link TextComponent#fromLegacy(String)} to parse legacy-formatted strings
     * and {@link BaseComponent#toLegacyText()} to serialize back to legacy format.</p>
     *
     * @since 1.0.0
     * @see BungeeChatSerializer
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    class BungeeLegacy implements BungeeChatSerializer {

        /**
         * Serializes the given {@link BaseComponent} array to a legacy section sign formatted string.
         *
         * <p>Concatenates the legacy text of each component in order.</p>
         *
         * @param component the component to serialize; never {@code null}
         * @return the legacy formatted string; never {@code null}
         */
        @Override
        public @NonNull String toLiteral(@NonNull BaseComponent component) {
            return component.toLegacyText();
        }

        /**
         * Parses a legacy section sign formatted string into a {@link BaseComponent} array.
         *
         * <p>Uses {@link TextComponent#fromLegacy(String)}, which consolidates all content
         * into a single {@link BaseComponent} with extras — preferred over the deprecated
         * {@link TextComponent#fromLegacy(String)}.</p>
         *
         * @param literal the legacy formatted string to parse; never {@code null}
         * @return a single-element {@link BaseComponent} array; never {@code null}
         */
        @Override
        public @NonNull BaseComponent fromLiteral(@NonNull String literal) {
            return TextComponent.fromLegacy(literal);
        }
    }
}