package de.leycm.i18label4j.serializer;

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.exception.*;
import de.leycm.i18label4j.label.LiteralLabel;
import de.leycm.i18label4j.label.LocaleLabel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import lombok.NonNull;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public interface AdventureComponentSerializer extends LabelSerializer<Component> {

    // ==== Adventure Interface ===============================================

    @Override
    default @NonNull Component serialize(final @NonNull Label label) throws SerializationException {
        try {
            if (label instanceof LocaleLabel locale) {
                return Component.translatable(locale.getKey(), locale.getFallback());
            } else if (label instanceof LiteralLabel literal) {
                return fromLiteral(literal.getLiteral());
            } else  {
                throw new SerializationException("Unsupported label type: " + label.getClass().getName());
            }
        } catch (Exception e) {
            throw new SerializationException(label, e);
        }
    }


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

    @Override
    default @NonNull Component format(@NonNull String input) throws FormatException {
        try {
            return fromLiteral(input);
        } catch (Exception e) {
            throw new FormatException(input, e);
        }
    }

    @NonNull String toLiteral(@NonNull Component component);

    @NonNull Component fromLiteral(@NonNull String literal);

    // ==== KyoriSerializer ===================================================

    class KyoriSerializer implements AdventureComponentSerializer {
        protected final ComponentSerializer<Component, ?, String> serializer;

        public KyoriSerializer(final @NonNull ComponentSerializer<Component,
                ?, String> serializer) {
            this.serializer = serializer;
        }

        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            return serializer.serialize(component);
        }

        @Override
        public @NonNull Component fromLiteral(@NonNull String literal) {
            return serializer.deserialize(literal);
        }

    }

    // ==== KyoriSerializer Types =============================================

    class KyoriMiniMessage extends KyoriSerializer {

        public KyoriMiniMessage() {
            super(MiniMessage.miniMessage());
        }

        public KyoriMiniMessage(final @NonNull MiniMessage miniMessage) {
            super(miniMessage);
        }
    }

    class KyoriLegacy extends KyoriSerializer {

        public KyoriLegacy() {
            super(LegacyComponentSerializer.legacySection());
        }

        public KyoriLegacy(final @NonNull LegacyComponentSerializer serializer) {
            super(LegacyComponentSerializer.legacySection());
        }
    }

    class KyoriJson extends KyoriSerializer {

        public KyoriJson() {
            super(JSONComponentSerializer.json());
        }

        public KyoriJson(final @NonNull JSONComponentSerializer serializer) {
            super(serializer);
        }
    }

    class KyoriPlainText extends KyoriSerializer {

        public KyoriPlainText() {
            super(PlainTextComponentSerializer.plainText());
        }

        public KyoriPlainText(final @NonNull PlainTextComponentSerializer serializer) {
            super(serializer);
        }
    }

}
