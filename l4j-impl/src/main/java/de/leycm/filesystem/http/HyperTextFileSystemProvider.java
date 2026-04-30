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

import de.leycm.filesystem.http.attribute.HyperTextBasicFileAttributeView;
import de.leycm.filesystem.http.attribute.HyperTextBasicFileAttributes;
import de.leycm.filesystem.http.channel.HyperTextSeekableByteChannel;
import de.leycm.filesystem.http.directory.HyperTextDirectoryStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HyperTextFileSystemProvider extends FileSystemProvider {
    private static final @NonNull String FILE_INDEXER = ".dir";
    private static final @NonNull String FILE_SEPARATOR = "/";
    private static final boolean READ_ONLY = true;

    private final @NonNull Map<URI, HyperTextFileSystem> fileSystems = new ConcurrentHashMap<>();
    private final @NonNull String scheme;

    protected HyperTextFileSystemProvider(final @NonNull String scheme) {
        this.scheme = scheme;
    }

    public @NonNull String getFileIndexer() {
        return FILE_INDEXER;
    }

    public @NonNull String getFileSeparator() {
        return FILE_SEPARATOR;
    }

    public boolean isReadOnly() {
        return READ_ONLY;
    }

    @Override
    public @NonNull String getScheme() {
        return scheme;
    }

    @Override
    public @NonNull FileSystem newFileSystem(
            final @NonNull URI uri,
            final @NonNull Map<String, ?> env
    ) {
        final String separator = getFileSeparator();
        final String domain = uri.getHost();
        final String path = uri.getPath();
        final String regex = "^" + separator + "|" + separator + "$";
        final String basePath = path.replaceAll(regex, "");
        final String[] pathSegments = basePath.isEmpty() ? new String[0] : basePath.split(separator);
        final Map<String, String> headers = new HashMap<>();
        env.forEach((key, value) -> headers.put(key, value != null ? String.valueOf(value) : null));

        final URI key = URI.create(scheme + "://" + domain);
        if (fileSystems.containsKey(key)) {
            throw new FileSystemAlreadyExistsException(key.toString());
        }

        final HyperTextFileSystem fs = new HyperTextFileSystem(this, domain, pathSegments, headers);
        fileSystems.put(key, fs);
        return fs;
    }

    @Override
    public @NonNull FileSystem getFileSystem(final @NonNull URI uri) {
        final URI key = URI.create(uri.getScheme() + "://" + uri.getHost());
        final HyperTextFileSystem fs = fileSystems.get(key);
        // note: create file system for root domain
        return Objects.requireNonNullElseGet(fs, () -> newFileSystem(key, Map.of()));
    }

    @Override
    public @NonNull Path getPath(final @NonNull URI uri) {
        final HyperTextFileSystem fs = (HyperTextFileSystem) getFileSystem(uri);
        final String path = uri.getPath() != null ? uri.getPath() : "/";
        return new HyperTextPath(fs, path);
    }

    @Override
    public @NonNull SeekableByteChannel newByteChannel(
            final @NonNull Path path,
            final @NonNull Set<? extends OpenOption> options,
            final @NonNull FileAttribute<?>... attrs
    ) throws IOException {
        for (final OpenOption option : options) {
            if (option != StandardOpenOption.READ) {
                throw new UnsupportedOperationException("Only READ is supported in a read-only file system");
            }
        }

        final HyperTextFileSystem fs = (HyperTextFileSystem) path.getFileSystem();
        final URI uri = path.toAbsolutePath().normalize().toUri();
        final HttpResponse<byte[]> response = fs.fetchBytes(uri);
        return new HyperTextSeekableByteChannel(response.body());
    }

    @Override
    public @NonNull DirectoryStream<Path> newDirectoryStream(
            final @NonNull Path dir,
            final @NonNull DirectoryStream.Filter<? super Path> filter
    ) throws IOException {
        final HyperTextFileSystem fs = (HyperTextFileSystem) dir.getFileSystem();
        final URI indexUri = buildIndexUri(fs, dir);
        final HttpResponse<byte[]> response = fs.fetchBytes(indexUri);
        final String body = new String(response.body());
        return new HyperTextDirectoryStream(dir, body, filter);
    }

    private @NonNull URI buildIndexUri(
            final @NonNull HyperTextFileSystem fs,
            final @NonNull Path dir
    ) {
        final Path abs = dir.toAbsolutePath().normalize();
        final URI base = abs.toUri();
        final String uriStr = base.toString();
        final String indexer = getFileIndexer();
        final String sep = getFileSeparator();
        final String indexUrl = uriStr.endsWith(sep)
                ? uriStr + indexer
                : uriStr + sep + indexer;
        return URI.create(indexUrl);
    }
    @Override
    public void createDirectory(final @NonNull Path dir, final @NonNull FileAttribute<?>... attrs) {
        throw new UnsupportedOperationException("Creating directories is not supported in a read-only file system");
    }

    @Override
    public void delete(final @NonNull Path path) {
        throw new UnsupportedOperationException("Deleting is not supported in a read-only file system");
    }

    @Override
    public void copy(final @NonNull Path source, final @NonNull Path target, final @NonNull CopyOption... options) {
        throw new UnsupportedOperationException("Copying is not supported in a read-only file system");
    }

    @Override
    public void move(final @NonNull Path source, final @NonNull Path target, final @NonNull CopyOption... options) {
        throw new UnsupportedOperationException("Moving is not supported in a read-only file system");
    }

    @Override
    public boolean isSameFile(final @NonNull Path path, final @NonNull Path path2) throws IOException {
        if (path.equals(path2)) return true;
        if (!(path.getFileSystem() instanceof HyperTextFileSystem)
                || !(path2.getFileSystem() instanceof HyperTextFileSystem)) return false;
        return path.toAbsolutePath().normalize().toUri()
                .equals(path2.toAbsolutePath().normalize().toUri());
    }

    @Override
    public boolean isHidden(final @NonNull Path path) {
        final Path fileName = path.getFileName();
        if (fileName == null) return false;
        return fileName.toString().startsWith(".");
    }

    @Override
    public FileStore getFileStore(final @NonNull Path path) {
        return null;
    }

    @Override
    public void checkAccess(
            final @NonNull Path path,
            final @NonNull AccessMode... modes
    ) throws IOException {
        final HyperTextPath hp = toHttpPath(path);
        final URI uri = hp.toUri();
        final HttpRequest request = hp.getFileSystem().newRequestBuilder(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        try {
            final HttpResponse<Void> response = hp.getFileSystem().getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            final int statusCode = response.statusCode();
            if (statusCode == 404) {
                final URI dotDirUri = uri.resolve(FILE_INDEXER);
                final HttpResponse<Void> dirCheck = hp.getFileSystem().getHttpClient()
                        .send(HttpRequest.newBuilder(dotDirUri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                                HttpResponse.BodyHandlers.discarding());
                final int dirStatusCode = dirCheck.statusCode();
                if (dirStatusCode == 404) {
                    throw new NoSuchFileException(path.toString());
                } else {
                    throw new IOException("HTTP " + dirStatusCode);
                }
            } else if (statusCode != 200) {
                throw new IOException("HTTP " + statusCode);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }


    @Override
    public <V extends FileAttributeView> @Nullable V getFileAttributeView(
            final @NonNull Path path,
            final @NonNull Class<V> type,
            final @NonNull LinkOption... options
    ) {
        if (type == BasicFileAttributeView.class) {
            @SuppressWarnings("unchecked")
            final V view = (V) new HyperTextBasicFileAttributeView(toHttpPath(path));
            return view;
        }
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> @NonNull A readAttributes(
            final @NonNull Path path,
            final @NonNull Class<A> type,
            final @NonNull LinkOption... options
    ) throws IOException {
        if (type == BasicFileAttributes.class) {
            @SuppressWarnings("unchecked")
            final A attrs = (A) readBasicAttributes(toHttpPath(path));
            return attrs;
        }
        throw new UnsupportedOperationException("BasicFileAttributes only");
    }

    @Override
    public @NonNull @Unmodifiable Map<String, Object> readAttributes(
            final @NonNull Path path,
            final @NonNull String attributes,
            final @NonNull LinkOption @NonNull ... options
    ) throws IOException {
        if (attributes.equals("*") || attributes.equals("basic:*")) {
            final BasicFileAttributes attrs = readBasicAttributes(toHttpPath(path));
            return Map.of(
                    "lastModifiedTime", attrs.lastModifiedTime(),
                    "lastAccessTime", attrs.lastAccessTime(),
                    "creationTime", attrs.creationTime(),
                    "size", attrs.size(),
                    "isRegularFile", attrs.isRegularFile(),
                    "isDirectory", attrs.isDirectory(),
                    "isSymbolicLink", attrs.isSymbolicLink(),
                    "isOther", attrs.isOther()
            );
        }
        throw new UnsupportedOperationException("Attribute: " + attributes);
    }

    @Override
    public void setAttribute(
            final @NonNull Path path,
            final @NonNull String attribute,
            final @Nullable Object value,
            final @NonNull LinkOption... options) {
        throw new ReadOnlyFileSystemException();
    }

    private @NonNull BasicFileAttributes readBasicAttributes(
            final @NonNull HyperTextPath path
    ) throws IOException {
        final HyperTextPath hp = toHttpPath(path);
        final URI uri = path.toUri();
        final HttpClient client = path.getFileSystem().getHttpClient();
        final HttpRequest headRequest = hp.getFileSystem().newRequestBuilder(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        try {
            final HttpResponse<Void> response = client.send(headRequest, HttpResponse.BodyHandlers.discarding());
            final int statusCode = response.statusCode();
            if (statusCode == 404) {
                final URI dotDirUri = URI.create(uri + getFileSeparator() + FILE_INDEXER);
                final HttpResponse<Void> dirResp = client.send(
                        HttpRequest.newBuilder(dotDirUri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.discarding());
                final int dirStatusCode = dirResp.statusCode();
                if (dirStatusCode == 200) {
                    return new HyperTextBasicFileAttributes(0L, true, null);
                } else {
                    throw new NoSuchFileException(path.toString());
                }
            } else if (statusCode != 200) {
                throw new IOException("HTTP " + statusCode);
            }
            final long size = response.headers().firstValueAsLong("Content-Length").orElse(0L);
            final String lastModified = response.headers().firstValue("Last-Modified").orElse(null);
            return new HyperTextBasicFileAttributes(size, false, lastModified);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private @NonNull HyperTextPath toHttpPath(final @NonNull Path path) {
        if (!(path instanceof final HyperTextPath httpPath)) {
            throw new ProviderMismatchException("Path is not matching provider");
        }
        return httpPath;
    }

}