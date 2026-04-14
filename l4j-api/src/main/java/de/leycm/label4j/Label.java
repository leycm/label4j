package de.leycm.label4j;

import de.leycm.label4j.exception.DuplicatePlaceholderException;
import de.leycm.label4j.placeholder.Placeholder;

import lombok.NonNull;

import java.util.function.Supplier;

public interface Label {

    @NonNull LabelProvider getProvider();


    default @NonNull Label replace(final @NonNull String key,
                           final @NonNull Object value) throws DuplicatePlaceholderException {
        return replace(key, () -> value);
    }

    default @NonNull Label replace(final @NonNull String key,
                           final @NonNull Supplier<Object> supplier) throws DuplicatePlaceholderException {
        return replace(new Placeholder(key, supplier));
    }

    @NonNull Label replace(@NonNull Placeholder mapping) throws DuplicatePlaceholderException;


    default @NonNull <T> T resolve(final @NonNull Class<T> type) {
        return getProvider().format(resolve(), type);
    }


    @NonNull String resolve();

    int hashCode();

    boolean equals(@NonNull Label label);

    @NonNull String toString();

}
