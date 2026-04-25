package de.leycm.label4j.exception;

import lombok.NonNull;

public class FlatParseException extends RuntimeException {
    public FlatParseException(final @NonNull String message) {
        super(message);
    }

    public FlatParseException(final @NonNull Throwable throwable) {
        super(throwable);
    }

    public FlatParseException(final @NonNull String message, final @NonNull Throwable throwable) {
        super(message, throwable);
    }
}
