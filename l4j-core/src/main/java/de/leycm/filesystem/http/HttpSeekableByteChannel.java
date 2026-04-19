package de.leycm.filesystem.http;

import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public final class HttpSeekableByteChannel implements SeekableByteChannel {
    private final byte @NonNull [] data;
    private int position = 0;
    private boolean open = true;

    public HttpSeekableByteChannel(final byte @NonNull [] data) {
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