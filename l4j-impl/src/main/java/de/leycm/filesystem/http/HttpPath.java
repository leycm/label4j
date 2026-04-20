package de.leycm.filesystem.http;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Objects;

public final class HttpPath implements Path {
    private final @NonNull HttpFileSystem fs;
    private final @NonNull String path;    // normalisiert, absolut, beginnt mit '/'

    public HttpPath(
            final @NonNull HttpFileSystem fs,
            final @NonNull String path) {
        this.fs = fs;
        this.path = path;
    }

    @Override
    public @NonNull HttpFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return path.startsWith("/");
    }

    @Override
    public @NotNull Path getRoot() {
        return path.equals("/") ? this : new HttpPath(fs, "/");
    }

    @Override
    public @Nullable Path getFileName() {
        if (path.equals("/")) return null;

        String normalizedPath = path;
        if (normalizedPath.endsWith("/") && normalizedPath.length() > 1) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        final int lastSlash = normalizedPath.lastIndexOf('/');

        final String name = normalizedPath.substring(lastSlash + 1);
        return name.isEmpty() ? null : new HttpPath(fs, name);
    }

    @Override
    public @Nullable Path getParent() {
        if (path.equals("/")) return null;
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash == 0) return new HttpPath(fs, "/");
        final String parent = path.substring(0, lastSlash);
        return new HttpPath(fs, parent);
    }

    @Override
    public int getNameCount() {
        if (path.equals("/")) {
            return 0;
        }
        final String[] parts = path.split("/");
        return parts.length;
    }

    @Override
    public @NonNull Path getName(final int index) {
        if (path.equals("/")) {
            throw new IllegalArgumentException();
        }
        final String[] parts = path.split("/");
        if (index < 0 || index >= parts.length) {
            throw new IllegalArgumentException();
        }
        return new HttpPath(fs, parts[index]);
    }

    @Override
    public @NonNull Path subpath(final int beginIndex, final int endIndex) {
        if (path.equals("/")) {
            throw new IllegalArgumentException();
        }
        final String[] parts = path.split("/");
        if (beginIndex < 0 || endIndex > parts.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(parts[i]);
        }
        return new HttpPath(fs, sb.toString());
    }

    @Override
    public boolean startsWith(final @NonNull Path other) {
        final String otherStr = other.toString();
        return path.equals(otherStr) || path.startsWith(otherStr + "/");
    }

    @Override
    public boolean startsWith(final @NonNull String other) {
        return path.equals(other) || path.startsWith(other + "/");
    }

    @Override
    public boolean endsWith(final @NonNull Path other) {
        final String otherStr = other.toString();
        return path.equals(otherStr) || path.endsWith("/" + otherStr);
    }

    @Override
    public boolean endsWith(final @NonNull String other) {
        return path.equals(other) || path.endsWith("/" + other);
    }

    @Override
    public @NonNull Path normalize() {
        return this;
    }

    @Override
    public @NonNull Path resolve(final @NonNull Path other) {
        if (other.isAbsolute()) {
            return other;
        }
        final String otherStr = other.toString();
        if (path.endsWith("/")) {
            return new HttpPath(fs, path + otherStr);
        } else {
            return new HttpPath(fs, path + "/" + otherStr);
        }
    }

    @Override
    public @NonNull Path resolve(final @NonNull String other) {
        return resolve(new HttpPath(fs, other));
    }

    @Override
    public @NonNull Path resolveSibling(final @NonNull Path other) {
        final Path parent = getParent();
        if (parent == null) {
            return other;
        }
        return parent.resolve(other);
    }

    @Override
    public @NonNull Path resolveSibling(final @NonNull String other) {
        return resolveSibling(new HttpPath(fs, other));
    }

    @Override
    public @NonNull Path relativize(final @NonNull Path other) {
        if (!(other instanceof final HttpPath httpOther)) {
            throw new IllegalArgumentException();
        }
        final String otherPath = httpOther.path;
        if (!otherPath.startsWith(path)) {
            throw new IllegalArgumentException();
        }
        String rel = otherPath.substring(path.length());
        if (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        if (rel.isEmpty()) {
            rel = ".";
        }
        return new HttpPath(fs, rel);
    }

    @Override
    public @NonNull URI toUri() {
        try {
            return new URI("htfp", null, fs.getBaseUri().getHost(), fs.getBaseUri().getPort(),
                    path, null, null);
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public @NonNull URI toHttpUri() {
        return fs.getBaseUri().resolve(path.startsWith("/") ? path.substring(1) : path);
    }

    @Override
    public @NonNull Path toAbsolutePath() {
        return this;
    }

    @Override
    public @NonNull Path toRealPath(
            final @NonNull LinkOption @NonNull ... options
    ) throws IOException {
        return this;
    }

    @Override
    public @NonNull WatchKey register(
            final @NonNull WatchService watcher,
            final @NonNull WatchEvent.Kind<?> @NonNull [] events,
            final @NonNull WatchEvent.Modifier @NonNull ... modifiers
    ) throws IOException {
        throw new IOException(new UnsupportedOperationException("WatchService not supported"));
    }

    @Override
    public @NonNull WatchKey register(
            final @NonNull WatchService watcher,
            final @NonNull WatchEvent.Kind<?> @NonNull ... events) throws IOException {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @Override
    public @NonNull File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(final @NonNull Path other) {
        if (!(other instanceof final HttpPath httpOther)) {
            throw new ClassCastException();
        }
        return path.compareTo(httpOther.path);
    }

    @Override
    public @NonNull String toString() {
        if (path.equals("/")) return fs.getBaseUri().toString();
        return fs.getBaseUri() + path;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final HttpPath that)) {
            return false;
        }
        return Objects.equals(fs, that.fs) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fs, path);
    }
}