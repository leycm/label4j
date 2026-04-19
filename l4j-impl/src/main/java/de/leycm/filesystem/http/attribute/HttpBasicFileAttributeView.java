package de.leycm.filesystem.http.attribute;

import de.leycm.filesystem.http.HttpPath;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public final class HttpBasicFileAttributeView implements BasicFileAttributeView {
    private final @NonNull HttpPath path;

    public HttpBasicFileAttributeView(final @NonNull HttpPath path) {
        this.path = path;
    }

    @Override
    public @NonNull String name() {
        return "basic";
    }

    @Override
    public @NonNull BasicFileAttributes readAttributes() throws IOException {
        return path.getFileSystem().provider().readAttributes(path, BasicFileAttributes.class);
    }

    @Override
    public void setTimes(
            final @NonNull FileTime lastModifiedTime,
            final @NonNull FileTime lastAccessTime,
            final @NonNull FileTime createTime) throws IOException {
        throw new IOException(new UnsupportedOperationException("Read-only"));
    }
}