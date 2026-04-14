package de.leycm.label4j;

import de.leycm.init4j.instance.Instanceable;
import de.leycm.label4j.exception.*;
import de.leycm.label4j.placeholder.PlaceholderRule;

import lombok.NonNull;

public interface LabelProvider extends Instanceable {

    static @NonNull LabelProvider getInstance() {
        return Instanceable.getInstance(LabelProvider.class);
    }

    @NonNull PlaceholderRule getDefaultPlaceholderRule();

    // ==== Serialization ====================================================

    <T> @NonNull T serialize(@NonNull Label label,
                             @NonNull Class<T> type)
            throws SerializationException;

    <T> @NonNull Label deserialize(@NonNull T serialized)
            throws DeserializationException;

    <T> @NonNull T format(@NonNull String input,
                          @NonNull Class<T> type)
            throws FormatException;
}
