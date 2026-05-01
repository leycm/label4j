/*
 * This file is part of label4j - https://github.com/leycm/label4j.
 * Copyright (C) 2026 Lennard [leycm] <leycm@proton.me>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.leycm.label4j.serializer;

import de.leycm.label4j.Label;
import de.leycm.label4j.LabelProvider;
import de.leycm.label4j.exception.DeserializationException;
import de.leycm.label4j.exception.SerializationException;
import de.leycm.label4j.label.ConstantLabel;
import de.leycm.label4j.label.LiteralLabel;
import de.leycm.label4j.label.LocaleLabel;
import de.leycm.label4j.locale.Locales;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import lombok.NonNull;

public class KyoriComponentAdapter implements LabelAdapter<Component> {

    // ==== Adapters ==========================================================

    static @NonNull KyoriComponentAdapter miniMessage(final @NonNull MiniMessage miniMessage) {
        return new KyoriComponentAdapter(miniMessage);
    }

    static @NonNull KyoriComponentAdapter miniMessage() {
        return miniMessage(MiniMessage.miniMessage());
    }

    static @NonNull KyoriComponentAdapter legacy(final @NonNull LegacyComponentSerializer serializer) {
        return new KyoriComponentAdapter(serializer);
    }

    static @NonNull KyoriComponentAdapter legacy(final char character) {
        return legacy(LegacyComponentSerializer.legacy(character));
    }

    static @NonNull KyoriComponentAdapter legacy() {
        return legacy(LegacyComponentSerializer.legacy(LegacyComponentSerializer.SECTION_CHAR));
    }

    static @NonNull KyoriComponentAdapter json(final @NonNull JSONComponentSerializer serializer) {
        return new KyoriComponentAdapter(serializer);
    }

    static @NonNull KyoriComponentAdapter json() {
        return json(JSONComponentSerializer.json());
    }

    static @NonNull KyoriComponentAdapter plain(final @NonNull PlainTextComponentSerializer serializer) {
        return new KyoriComponentAdapter(serializer);
    }

    static @NonNull KyoriComponentAdapter plain() {
        return plain(PlainTextComponentSerializer.plainText());
    }

    private final @NonNull ComponentSerializer<Component, ?, String> serializer;

    public KyoriComponentAdapter(
            final @NonNull ComponentSerializer<Component, ?, String> serializer
    ) {
        this.serializer = serializer;
    }

    // ==== Adapting ==========================================================

    @Override
    public @NonNull Component serialize(
            final @NonNull Label label
    ) throws SerializationException {
        try {
            return switch (label) {
                case final ConstantLabel constant
                        -> Component.translatable(constant.getKey());
                case final LocaleLabel locale
                        -> Component.translatable(locale.getKey());
                case final LiteralLabel literal
                        -> fromString(literal.resolveDefault());
                default -> throw new SerializationException("Unsupported label type: "
                                + label.getClass().getName());
            };
        } catch (Exception e) {
            throw new SerializationException(label, e);
        }
    }

    @Override
    public @NonNull Label deserialize(
            final @NonNull Component serialized,
            final @NonNull LabelProvider provider
    ) throws DeserializationException {
        try {
            if (serialized instanceof final TranslatableComponent translatable) {
                final String fallback = translatable.fallback();
                if (fallback != null && fallback.equalsIgnoreCase(Locales.CONSTANT_FILE_TAG))
                    return Label.constant(translatable.key(), provider);
                return Label.locale(translatable.key(), provider);
            } else {
                return Label.literal(toString(serialized), provider);
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    @Override
    public @NonNull String toString(
            final @NonNull Component component
    ) throws Exception {
        return serializer.serialize(component);
    }

    @Override
    public @NonNull Component fromString(
            final @NonNull String string
    ) throws Exception {
        return serializer.deserialize(string);
    }
}
