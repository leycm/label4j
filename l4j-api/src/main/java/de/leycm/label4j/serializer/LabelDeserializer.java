package de.leycm.label4j.serializer;

import de.leycm.label4j.Label;
import de.leycm.label4j.LabelProvider;
import de.leycm.label4j.exception.DeserializationException;

import lombok.NonNull;

@FunctionalInterface
public interface LabelDeserializer<T> {

    @NonNull Label deserialize(@NonNull T serialized, @NonNull LabelProvider provider)
            throws DeserializationException;
}

