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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import lombok.NonNull;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.Contract;

import java.util.function.Function;

public class BungeeChatAdapter implements LabelAdapter<BaseComponent> {

    @Contract("_, _ -> new")
    private static @NonNull String translateFromAlternateColorCodes(final char altColorChar, final @NonNull String textToTranslate) {
        return ChatColor.translateAlternateColorCodes(altColorChar, textToTranslate);
    }

    @Contract("_, _ -> new")
    // note: this method is a reversion of ChatColor#translateAlternateColorCodes(char, String) all credits to md_5
    private static @NonNull String translateToAlternateColorCodes(final char targetColorChar, final @NonNull String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == ChatColor.COLOR_CHAR && ChatColor.ALL_CODES.indexOf(b[i + 1]) > -1) {
                b[i] = targetColorChar;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    // note: bungee don't support miniMessage you need Kyori as an translator
    static @NonNull BungeeChatAdapter miniMessage(final @NonNull MiniMessage serializer) {
        final Function<String, BaseComponent> fromString = str -> {
            final Component adventure = serializer.deserialize(str);
            final String json = JSONComponentSerializer.json().serialize(adventure);
            return ComponentSerializer.deserialize(json);
        };

        final Function<BaseComponent, String> toString = component -> {
            final String json = ComponentSerializer.toString(component);
            final Component adventure = JSONComponentSerializer.json().deserialize(json);
            return serializer.serialize(adventure);
        };

        return new BungeeChatAdapter(toString, fromString);
    }

    // note: bungee don't support miniMessage you need Kyori as an translator
    static @NonNull BungeeChatAdapter miniMessage() {
        return miniMessage(MiniMessage.miniMessage());
    }

    static @NonNull BungeeChatAdapter legacy(final char character) {
        final Function<String, BaseComponent> fromString = str -> {
            final String converted = translateFromAlternateColorCodes(character, str);
            return TextComponent.fromLegacy(converted);
        };

        final Function<BaseComponent, String> toString = component -> {
            final String legacy = component.toLegacyText(); // contains §
            return translateToAlternateColorCodes(character, legacy);
        };

        return new BungeeChatAdapter(toString, fromString);
    }

    static @NonNull BungeeChatAdapter legacy() {
        return new BungeeChatAdapter(component -> component.toLegacyText(), TextComponent::fromLegacy);
    }

    static @NonNull BungeeChatAdapter json() {
        return new BungeeChatAdapter(ComponentSerializer::toString, ComponentSerializer::deserialize);
    }

    static @NonNull BungeeChatAdapter plain() {
        return new BungeeChatAdapter(component -> component.toPlainText(), TextComponent::new);
    }

    private final @NonNull Function<BaseComponent, String> toString;
    private final @NonNull Function<String, BaseComponent> fromString;

    public BungeeChatAdapter(
            final @NonNull Function<BaseComponent, String> toString,
            final @NonNull Function<String, BaseComponent> fromString
    ) {
        this.toString = toString;
        this.fromString = fromString;
    }

    @Override
    public @NonNull BaseComponent serialize(
            final @NonNull Label label
    ) throws SerializationException {
        try {
            return switch (label) {
                case final ConstantLabel constant
                        -> new TranslatableComponent(constant.getKey(), Locales.CONSTANT_FILE_TAG);
                case final LocaleLabel locale
                        -> new TranslatableComponent(locale.getKey());
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
            final @NonNull BaseComponent serialized,
            final @NonNull LabelProvider provider
    ) throws DeserializationException {
        try {
            if (serialized instanceof final TranslatableComponent translatable) {
                final String fallback = translatable.getFallback();
                if (fallback != null && fallback.equalsIgnoreCase(Locales.CONSTANT_FILE_TAG))
                    return Label.constant(translatable.getTranslate(), provider);
                return Label.locale(translatable.getTranslate(), provider);
            } else {
                return Label.literal(toString(serialized), provider);
            }
        } catch (Exception e) {
            throw new DeserializationException(serialized, e);
        }
    }

    @Override
    public @NonNull String toString(
            final @NonNull BaseComponent component
    ) throws Exception {
        return toString.apply(component);
    }

    @Override
    public @NonNull BaseComponent fromString(
            final @NonNull String string
    ) throws Exception {
        return fromString.apply(string);
    }
}
