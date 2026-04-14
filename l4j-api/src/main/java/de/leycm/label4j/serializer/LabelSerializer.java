package de.leycm.label4j.serializer;

import de.leycm.label4j.Label;
import de.leycm.label4j.exception.SerializationException;

import lombok.NonNull;

@FunctionalInterface
public interface LabelSerializer<T> {

    @NonNull T serialize(@NonNull Label label) throws SerializationException;
}
