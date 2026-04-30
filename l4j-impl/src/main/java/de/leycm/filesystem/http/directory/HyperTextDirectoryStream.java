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
package de.leycm.filesystem.http.directory;

import lombok.NonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class HyperTextDirectoryStream implements DirectoryStream<Path> {
    private final @NonNull Path dir;
    private final @NonNull String[] entries;
    private final @NonNull DirectoryStream.Filter<? super Path> filter;
    private boolean closed = false;

    public HyperTextDirectoryStream(
            final @NonNull Path dir,
            final @NonNull String dotDirContent,
            final @NonNull DirectoryStream.Filter<? super Path> filter) {
        this.dir = dir;
        this.filter = filter;
        this.entries = dotDirContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toArray(String[]::new);
    }

    @Override
    public @NonNull Iterator<Path> iterator() {
        if (closed) {
            throw new IllegalStateException("Stream already closed");
        }

        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                if (closed) return false;
                while (index < entries.length) {
                    final Path p = dir.resolve(entries[index]);
                    try {
                        if (filter.accept(p)) return true;
                    } catch (final IOException ignored) {
                    }
                    index++;
                }
                return false;
            }

            @Override
            public @NonNull Path next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final Path result = dir.resolve(entries[index]);
                index++;
                return result;
            }
        };
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}