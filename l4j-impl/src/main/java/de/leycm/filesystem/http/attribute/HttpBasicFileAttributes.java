package de.leycm.filesystem.http.attribute;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class HttpBasicFileAttributes implements BasicFileAttributes {
    private final @NonNull FileTime lastModified;
    private final long size;
    private final boolean directory;

    public HttpBasicFileAttributes(
            final long size,
            final boolean directory,
            final @Nullable String lastModifiedHeader) {
        this.size = size;
        this.directory = directory;
        this.lastModified = parseLastModified(lastModifiedHeader);
    }

    private static @NonNull FileTime parseLastModified(
            final @Nullable String header
    ) {
        if (header == null) {
            return FileTime.from(Instant.EPOCH);
        }
        try {
            final ZonedDateTime zdt = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME);
            return FileTime.from(zdt.toInstant());
        } catch (final DateTimeParseException e) {
            return FileTime.from(Instant.EPOCH);
        }
    }

    @Override
    public @NonNull FileTime lastModifiedTime() {
        return lastModified;
    }

    @Override
    public @NonNull FileTime lastAccessTime() {
        return lastModified;
    }

    @Override
    public @NonNull FileTime creationTime() {
        return lastModified;
    }

    @Override
    public boolean isRegularFile() {
        return !directory;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public @Nullable Object fileKey() {
        return null;
    }
}