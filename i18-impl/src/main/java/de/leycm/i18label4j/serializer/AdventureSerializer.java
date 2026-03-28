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
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public interface AdventureSerializer extends LabelSerializer<Component> {

    @Override
    default @NonNull Component serialize(final @NonNull Label label) throws SerializationException {
        if (label instanceof LiteralLabel literal) {
            return Component.text(literal.getLiteral());
        } else if (label instanceof LocaleLabel locale) {
            return Component.translatable(locale.getKey(), locale.getFallback());
        } else {
            throw new SerializationException("Unsupported label type: " + label.getClass().getName());
        }
    }


    @Override
    default @NonNull Label deserialize(final @NonNull Component serialized, final @NonNull LabelProvider provider) throws DeserializationException {
        if (serialized instanceof TranslatableComponent translatable) {
            final String fallback = translatable.fallback();
            if (fallback == null) return Label.of(provider, translatable.key());
            return Label.of(provider, translatable.key(), fallback);
        } else {
            return Label.literal(provider, toLiteral(serialized));
        }
    }

    @NonNull String toLiteral(@NonNull Component component);

    class KyoriMiniMessage implements AdventureSerializer {
        private static final MiniMessage INSTANCE = MiniMessage.miniMessage();

        private final MiniMessage miniMessage;

        public KyoriMiniMessage() {
            this(INSTANCE);
        }

        public KyoriMiniMessage(final @NonNull MiniMessage miniMessage) {
            this.miniMessage = miniMessage;
        }

        @Override
        public @NonNull Component format(@NonNull String input) throws FormatException {
            return miniMessage.deserialize(input);
        }

        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            return miniMessage.serialize(component);
        }
    }

    class KyoriLegacy implements AdventureSerializer {

        private static final LegacyComponentSerializer INSTANCE =
                LegacyComponentSerializer.legacySection();

        private final LegacyComponentSerializer serializer;

        public KyoriLegacy() {
            this(INSTANCE);
        }

        public KyoriLegacy(final @NonNull LegacyComponentSerializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public @NonNull Component format(@NonNull String input) throws FormatException {
            return serializer.deserialize(input);
        }

        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            return serializer.serialize(component);
        }
    }

    class KyoriJson implements AdventureSerializer {

        private static final JSONComponentSerializer INSTANCE =
                JSONComponentSerializer.json();

        private final JSONComponentSerializer serializer;

        public KyoriJson() {
            this(INSTANCE);
        }

        public KyoriJson(final @NonNull JSONComponentSerializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public @NonNull Component format(@NonNull String input) throws FormatException {
            return serializer.deserialize(input);
        }

        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            return serializer.serialize(component);
        }
    }

    class KyoriPlainText implements AdventureSerializer {

        private static final PlainTextComponentSerializer INSTANCE =
                PlainTextComponentSerializer.plainText();

        private final PlainTextComponentSerializer serializer;

        public KyoriPlainText() {
            this(INSTANCE);
        }

        public KyoriPlainText(final @NonNull PlainTextComponentSerializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public @NonNull Component format(@NonNull String input) throws FormatException {
            return Component.text(input);
        }

        @Override
        public @NonNull String toLiteral(@NonNull Component component) {
            return serializer.serialize(component);
        }
    }

}
