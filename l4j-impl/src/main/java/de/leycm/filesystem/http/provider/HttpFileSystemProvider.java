package de.leycm.filesystem.http.provider;

import de.leycm.filesystem.http.*;
import de.leycm.filesystem.http.attribute.HttpBasicFileAttributeView;
import de.leycm.filesystem.http.attribute.HttpBasicFileAttributes;
import de.leycm.filesystem.http.channel.HttpSeekableByteChannel;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HttpFileSystemProvider extends FileSystemProvider {
    private final @NonNull Map<URI, HttpFileSystem> fileSystems = new ConcurrentHashMap<>();
    private final @NonNull String scheme; // htfp / htfps
    private final @NonNull String type; // http / https

    public HttpFileSystemProvider() {
        // called via META-INF/services/java.nio.file.spi.FileSystemProvider
        this.scheme = "htfp";
        this.type = "http";
    }

    public HttpFileSystemProvider(@NonNull String scheme, @NonNull String type) {
        this.scheme = scheme;
        this.type = type;
    }

    @Override
    public @NonNull String getScheme() {
        return scheme;
    }

    @Override
    public @NonNull FileSystem newFileSystem(
            final @NonNull URI uri,
            final @NonNull Map<String, ?> env) throws IOException {
        final URI httpUri = convertToHttpUri(uri);
        final String indexFile = ".dir";
        final HttpFileSystem fs = new HttpFileSystem(this, httpUri, indexFile);
        fileSystems.put(uri, fs);
        return fs;
    }

    @Override
    public @NonNull FileSystem getFileSystem(final @NonNull URI uri) {
        final HttpFileSystem fs = fileSystems.get(uri);
        if (fs == null) {
            throw new FileSystemNotFoundException(uri.toString());
        }
        return fs;
    }

    @Override
    public @NonNull Path getPath(final @NonNull URI uri) {
        final URI withoutScheme;
        try {
            withoutScheme = new URI(type, uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        final HttpFileSystem fs = (HttpFileSystem) getFileSystem(URI.create(uri.getScheme() + "://" + uri.getAuthority()));
        String path = withoutScheme.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        return fs.getPath(path);
    }

    @Override
    public @NonNull SeekableByteChannel newByteChannel(
            final @NonNull Path path,
            final @NonNull Set<? extends OpenOption> options,
            final @NonNull FileAttribute<?>... attrs) throws IOException {
        checkReadOnly(options);
        final HttpPath hp = toHTTPPath(path);
        final URI fileUri = hp.toHttpUri();
        final HttpRequest request = HttpRequest.newBuilder(fileUri).GET().build();
        try {
            final HttpResponse<byte[]> response = hp.getFileSystem().getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            final int statusCode = response.statusCode();
            if (statusCode == 404) {
                throw new NoSuchFileException(path.toString());
            }
            if (statusCode != 200) {
                throw new IOException(type.toUpperCase() + " " + statusCode + " for " + fileUri);
            }
            final byte[] data = response.body();
            return new HttpSeekableByteChannel(data);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public @NonNull DirectoryStream<Path> newDirectoryStream(
            final @NonNull Path dir,
            final @NonNull DirectoryStream.Filter<? super Path> filter) throws IOException {
        final HttpPath directory = toHTTPPath(dir);
        final URI dotDirUri = directory.toHttpUri().resolve(directory.getFileSystem().getIndexFile());

        final HttpRequest request = HttpRequest.newBuilder(dotDirUri).GET().build();
        try {
            final HttpResponse<String> response = directory.getFileSystem().getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            if (statusCode == 404) {
                throw new NotDirectoryException(dir.toString());
            }
            if (statusCode != 200) {
                throw new IOException(type.toUpperCase() + " " + statusCode + " for " + dotDirUri);
            }
            return new HttpDirectoryStream(directory, response.body(), filter);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void createDirectory(
            final @NonNull Path dir,
            final @NonNull FileAttribute<?>... attrs) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void delete(final @NonNull Path path) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void copy(
            final @NonNull Path source,
            final @NonNull Path target,
            final @NonNull CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void move(
            final @NonNull Path source,
            final @NonNull Path target,
            final @NonNull CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public boolean isSameFile(
            final @NonNull Path path,
            final @NonNull Path path2) throws IOException {
        return path.toAbsolutePath().equals(path2.toAbsolutePath());
    }

    @Override
    public boolean isHidden(final @NonNull Path path) throws IOException {
        final Path name = path.getFileName();
        return name != null && name.toString().startsWith(".");
    }

    @Override
    public @NonNull FileStore getFileStore(final @NonNull Path path) throws IOException {
        throw new IOException(new UnsupportedOperationException("getFileStore not supported"));
    }

    @Override
    public void checkAccess(
            final @NonNull Path path,
            final @NonNull AccessMode... modes
    ) throws IOException {
        final HttpPath hp = toHTTPPath(path);
        final URI uri = hp.toHttpUri();
        final HttpRequest request = HttpRequest.newBuilder(uri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        try {
            final HttpResponse<Void> response = hp.getFileSystem().getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            final int statusCode = response.statusCode();
            if (statusCode == 404) {
                final URI dotDirUri = uri.resolve(hp.getFileSystem().getIndexFile());
                final HttpResponse<Void> dirCheck = hp.getFileSystem().getHttpClient()
                        .send(HttpRequest.newBuilder(dotDirUri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                                HttpResponse.BodyHandlers.discarding());
                final int dirStatusCode = dirCheck.statusCode();
                if (dirStatusCode == 404) {
                    throw new NoSuchFileException(path.toString());
                } else if (dirStatusCode == 200) {
                    return;
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
            final @NonNull LinkOption... options) {
        if (type == BasicFileAttributeView.class) {
            @SuppressWarnings("unchecked")
            final V view = (V) new HttpBasicFileAttributeView(toHTTPPath(path));
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
            final A attrs = (A) readBasicAttributes(toHTTPPath(path));
            return attrs;
        }
        throw new IOException(new UnsupportedOperationException("BasicFileAttributes only"));
    }

    @Override
    public @NonNull @Unmodifiable Map<String, Object> readAttributes(
            final @NonNull Path path,
            final @NonNull String attributes,
            final @NonNull LinkOption @NonNull ... options) throws IOException {
        if (attributes.equals("*") || attributes.equals("basic:*")) {
            final BasicFileAttributes attrs = readBasicAttributes(toHTTPPath(path));
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
        throw new IOException(new UnsupportedOperationException("Attribut: " + attributes));
    }

    @Override
    public void setAttribute(
            final @NonNull Path path,
            final @NonNull String attribute,
            final @Nullable Object value,
            final @NonNull LinkOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    private @NonNull HttpPath toHTTPPath(final @NonNull Path path) {
        if (!(path instanceof final HttpPath httpPath)) {
            throw new ProviderMismatchException("Path gehört nicht zu diesem Provider");
        }
        return httpPath;
    }

    private @NonNull BasicFileAttributes readBasicAttributes(final @NonNull HttpPath path) throws IOException {
        final URI uri = path.toHttpUri();
        final HttpClient client = path.getFileSystem().getHttpClient();
        final HttpRequest headRequest = HttpRequest.newBuilder(uri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        try {
            final HttpResponse<Void> response = client.send(headRequest, HttpResponse.BodyHandlers.discarding());
            final int statusCode = response.statusCode();
            if (statusCode == 404) {
                final URI dotDirUri = uri.resolve(path.getFileSystem().getIndexFile());
                final HttpResponse<Void> dirResp = client.send(
                        HttpRequest.newBuilder(dotDirUri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.discarding());
                final int dirStatusCode = dirResp.statusCode();
                if (dirStatusCode == 200) {
                    return new HttpBasicFileAttributes(0L, true, null);
                } else {
                    throw new NoSuchFileException(path.toString());
                }
            } else if (statusCode != 200) {
                throw new IOException("HTTP " + statusCode);
            }
            final long size = response.headers().firstValueAsLong("Content-Length").orElse(0L);
            final String lastModified = response.headers().firstValue("Last-Modified").orElse(null);
            return new HttpBasicFileAttributes(size, false, lastModified);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private @NonNull URI convertToHttpUri(final @NonNull URI htfpUri) {
        try {
            return new URI("http", htfpUri.getUserInfo(), htfpUri.getHost(), htfpUri.getPort(),
                    htfpUri.getPath(), htfpUri.getQuery(), htfpUri.getFragment());
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void checkReadOnly(final @NonNull Set<? extends OpenOption> options) {
        if (options.contains(StandardOpenOption.WRITE) ||
                options.contains(StandardOpenOption.APPEND) ||
                options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
            throw new ReadOnlyFileSystemException();
        }
    }
}