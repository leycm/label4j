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
package de.leycm.filesystem.http.channel;

import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public final class HyperTextSeekableByteChannel implements SeekableByteChannel {
    private final byte @NonNull [] data;
    private int position = 0;
    private boolean open = true;

    public HyperTextSeekableByteChannel(final byte @NonNull [] data) {
        this.data = data.clone();
    }

    @Override
    public int read(final @NonNull ByteBuffer dst) throws IOException {
        if (!open) {
            throw new IOException("Channel closed");
        }
        final int remaining = data.length - position;
        if (remaining <= 0) {
            return -1;
        }
        final int toRead = Math.min(dst.remaining(), remaining);
        dst.put(data, position, toRead);
        position += toRead;
        return toRead;
    }

    @Override
    public int write(final @NonNull ByteBuffer src) throws IOException {
        throw new IOException(new UnsupportedOperationException("Read-only"));
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public @NonNull SeekableByteChannel position(final long newPosition) throws IOException {
        if (newPosition < 0 || newPosition > data.length) {
            throw new IOException("Position out of range");
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return data.length;
    }

    @Override
    public @NonNull SeekableByteChannel truncate(final long size) throws IOException {
        throw new IOException(new UnsupportedOperationException("Read-only"));
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }
}