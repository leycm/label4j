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
package de.leycm.filesystem.http.attribute;

import de.leycm.filesystem.http.HyperTextPath;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public final class HyperTextBasicFileAttributeView implements BasicFileAttributeView {
    private final @NonNull HyperTextPath path;

    public HyperTextBasicFileAttributeView(final @NonNull HyperTextPath path) {
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