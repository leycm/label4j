/*
 * This file is part of label4j - https://github.com/leycm/label4j.
 * Copyright (C) 2026 Lennard [leycm] <leycm@proton.me>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.leycm.filesystem.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;

public class HyperTextPath implements Path {
    private final @NonNull HyperTextFileSystem fileSystem;
    /** Path segments – never contains empty strings except for the root path. */
    private final @NonNull String[] segments;
    /** True when the path was created with a leading separator. */
    private final boolean absolute;

    public HyperTextPath(
            final @NonNull HyperTextFileSystem fileSystem,
            final @NonNull String[] segments,
            final boolean absolute
    ) {
        this.fileSystem = fileSystem;
        this.segments = segments;
        this.absolute = absolute;
    }

    public HyperTextPath(
            final @NonNull HyperTextFileSystem fileSystem,
            final @NonNull String rawPath
    ) {
        this.fileSystem = fileSystem;
        final String sep = fileSystem.getSeparator();
        this.absolute = rawPath.startsWith(sep);
        final String stripped = rawPath.replaceAll("^" + sep + "+|" + sep + "+$", "");
        this.segments = stripped.isEmpty() ? new String[0] : stripped.split(sep, -1);
    }

    @Override
    public @NonNull HyperTextFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public @Nullable Path getRoot() {
        return absolute ? new HyperTextPath(fileSystem, new String[0], true) : null;
    }

    @Override
    public @Nullable Path getFileName() {
        if (segments.length == 0) return null;
        return new HyperTextPath(fileSystem, new String[]{segments[segments.length - 1]}, false);
    }

    @Override
    public @Nullable Path getParent() {
        if (segments.length == 0) return null;
        if (segments.length == 1) {
            return absolute ? new HyperTextPath(fileSystem, new String[0], true) : null;
        }
        return new HyperTextPath(fileSystem, Arrays.copyOf(segments, segments.length - 1), absolute);
    }

    @Override
    public int getNameCount() {
        return segments.length;
    }

    @Override
    public @NonNull Path getName(int index) {
        if (index < 0 || index >= segments.length) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }
        return new HyperTextPath(fileSystem, new String[]{segments[index]}, false);
    }

    @Override
    public @NonNull Path subpath(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > segments.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException(
                    "Invalid range [" + beginIndex + ", " + endIndex + ") for length " + segments.length);
        }
        return new HyperTextPath(fileSystem,
                Arrays.copyOfRange(segments, beginIndex, endIndex),
                false /* subpath is always relative */);
    }

    @Override
    public boolean startsWith(@NonNull Path other) {
        if (!(other instanceof HyperTextPath o) || !fileSystem.equals(o.fileSystem)) return false;
        if (absolute != o.absolute) return false;
        if (o.segments.length > segments.length) return false;
        for (int i = 0; i < o.segments.length; i++) {
            if (!segments[i].equals(o.segments[i])) return false;
        }
        return true;
    }

    @Override
    public boolean endsWith(@NonNull Path other) {
        if (!(other instanceof HyperTextPath o) || !fileSystem.equals(o.fileSystem)) return false;
        if (o.absolute && !absolute) return false;
        if (o.segments.length > segments.length) return false;
        int offset = segments.length - o.segments.length;
        for (int i = 0; i < o.segments.length; i++) {
            if (!segments[offset + i].equals(o.segments[i])) return false;
        }
        return true;
    }

    @Override
    public @NonNull Path normalize() {
        final List<String> normalized = new ArrayList<>();
        for (final String seg : segments) {
            switch (seg) {
                case "." -> {}
                case ".." -> {
                    if (!normalized.isEmpty()) {
                        normalized.removeLast();
                    } else if (!absolute) {
                        normalized.add("..");
                    }
                }
                default -> normalized.add(seg);
            }
        }
        return new HyperTextPath(fileSystem, normalized.toArray(String[]::new), absolute);
    }

    @Override
    public @NonNull Path resolve(@NonNull Path other) {
        if (!(other instanceof HyperTextPath o)) {
            throw new ProviderMismatchException("Cannot resolve path from a different provider");
        }
        // If other is absolute, it replaces this path entirely.
        if (o.isAbsolute()) return o;
        // If other is empty, return this.
        if (o.segments.length == 0) return this;

        final String[] combined = new String[segments.length + o.segments.length];
        System.arraycopy(segments, 0, combined, 0, segments.length);
        System.arraycopy(o.segments, 0, combined, segments.length, o.segments.length);
        return new HyperTextPath(fileSystem, combined, absolute);
    }

    @Override
    public @NonNull Path relativize(@NonNull Path other) {
        if (!(other instanceof HyperTextPath o)) {
            throw new ProviderMismatchException("Cannot relativize path from a different provider");
        }

        if (absolute != o.absolute) {
            throw new IllegalArgumentException("Both paths must be of the same type (absolute/relative)");
        }

        final List<String> rel = result(o);

        return new HyperTextPath(fileSystem, rel.toArray(String[]::new), false);
    }

    private @NonNull List<String> result(@NonNull HyperTextPath o) {
        int common = 0;
        final int minLen = Math.min(segments.length, o.segments.length);
        while (common < minLen && segments[common].equals(o.segments[common])) common++;

        final List<String> result = new ArrayList<>();
        for (int i = common; i < segments.length; i++) result.add("..");
        result.addAll(Arrays.asList(o.segments).subList(common, o.segments.length));
        return result;
    }

    @Override
    public @NonNull URI toUri() {
        final String scheme = fileSystem.provider().getScheme();
        final String domain = fileSystem.getDomain();
        final String[] base = fileSystem.getBasepath();
        final String sep = fileSystem.getSeparator();

        final StringBuilder sb = new StringBuilder(sep);
        for (final String seg : base) sb.append(seg).append(sep);
        for (int i = 0; i < segments.length; i++) {
            sb.append(segments[i]);
            if (i < segments.length - 1) sb.append(sep);
        }

        try {
            return new URI(scheme, domain, sb.toString(), null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot build URI for path: " + this, e);
        }
    }

    @Override
    public @NonNull Path toAbsolutePath() {
        if (absolute) return this;
        final String[] base = fileSystem.getBasepath();
        final String[] abs = new String[base.length + segments.length];
        System.arraycopy(base, 0, abs, 0, base.length);
        System.arraycopy(segments, 0, abs, base.length, segments.length);
        return new HyperTextPath(fileSystem, abs, true);
    }

    @Override
    public @NonNull Path toRealPath(@NonNull @NotNull LinkOption @NonNull ... options) {
        return toAbsolutePath().normalize();
    }

    @Override
    public @NonNull WatchKey register(
            @NonNull WatchService watcher,
            WatchEvent.@NonNull Kind<?> @NonNull [] events,
            WatchEvent.@NonNull Modifier @NonNull ... modifiers
    ) {
        throw new UnsupportedOperationException("WatchService is not supported for HTTP paths");
    }

    @Override
    public @NonNull Iterator<Path> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < segments.length;
            }

            @Override
            public Path next() {
                if (!hasNext()) throw new NoSuchElementException();
                return new HyperTextPath(fileSystem, new String[]{segments[index++]}, false);
            }
        };
    }

    @Override
    public int compareTo(@NonNull Path other) {
        return toString().compareTo(other.toString());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HyperTextPath o)) return false;
        return absolute == o.absolute
                && fileSystem.equals(o.fileSystem)
                && Arrays.equals(segments, o.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileSystem, Arrays.hashCode(segments), absolute);
    }


    @Override
    public @NonNull String toString() {
        final String sep = fileSystem.getSeparator();
        final String joined = String.join(sep, segments);
        return absolute ? sep + joined : joined;
    }
}
