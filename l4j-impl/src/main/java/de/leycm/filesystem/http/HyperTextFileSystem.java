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

import lombok.NonNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class HyperTextFileSystem extends FileSystem {
    private final @NonNull AtomicBoolean closed = new AtomicBoolean(false);
    private final @NonNull HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final @NonNull HyperTextFileSystemProvider provider;
    private final @NonNull String domain;
    private final @NonNull String[] basepath;
    private final @NonNull Map<String, String> headers;

    public HyperTextFileSystem(
            final @NonNull HyperTextFileSystemProvider provider,
            final @NonNull String domain,
            final @NonNull String[] basepath,
            final @NonNull Map<String, String> headers
    ) {
        this.provider = provider;
        this.domain = domain;
        this.basepath = basepath;
        this.headers = new ConcurrentHashMap<>(headers);
    }

    public @NonNull String getDomain() {
        return domain;
    }

    public @NonNull HyperTextFileSystemProvider getProvider() {
        return provider;
    }

    public @NonNull String[] getBasepath() {
        return basepath;
    }

    public @NonNull HttpClient getHttpClient() {
        return httpClient;
    }

    public @NonNull Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public @NonNull HttpRequest.Builder newRequestBuilder(final @NonNull URI uri) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        headers.forEach(builder::header);
        return builder;
    }

    public @NonNull HttpResponse<byte[]> fetchBytes(final @NonNull URI uri) throws IOException {
        final HttpRequest request = newRequestBuilder(uri)
                .GET()
                .build();
        try {
            final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }
            return response;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted: " + uri, e);
        }
    }

    public long fetchContentLength(final @NonNull URI uri) throws IOException {
        final HttpRequest request = newRequestBuilder(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP HEAD " + response.statusCode() + " for " + uri);
            }
            return response.headers()
                    .firstValueAsLong("content-length")
                    .orElse(-1L);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HEAD request interrupted: " + uri, e);
        }
    }

    public boolean resourceExists(final @NonNull URI uri) {
        final HttpRequest request = newRequestBuilder(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (final IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public @NonNull FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            httpClient.close();
            return;
        }
        throw new IOException(new IllegalStateException("FileSystem is already closed"));
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public boolean isReadOnly() {
        return provider.isReadOnly();
    }

    @Override
    public @NonNull String getSeparator() {
        return provider.getFileSeparator();
    }

    @Override
    public @NonNull Iterable<Path> getRootDirectories() {
        return Collections.singleton(getPath(getSeparator()));
    }

    @Override
    public @NonNull Iterable<FileStore> getFileStores() {
        return Collections.emptySet();
    }

    @Override
    public @NonNull Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    @Override
    public @NonNull Path getPath(final @NonNull String first, final @NonNull String @NonNull ... more) {
        final String sep = getSeparator();
        final StringBuilder raw = new StringBuilder(first);
        for (final String part : more) {
            if (!raw.isEmpty() && !raw.toString().endsWith(sep) && !part.startsWith(sep)) {
                raw.append(sep);
            }
            raw.append(part);
        }
        return new HyperTextPath(this, raw.toString());
    }

    @Override
    public @NonNull PathMatcher getPathMatcher(final @NonNull String syntaxAndPattern) {
        final int colonIndex = syntaxAndPattern.indexOf(':');
        if (colonIndex <= 0) {
            throw new IllegalArgumentException("Invalid syntaxAndPattern: " + syntaxAndPattern);
        }
        final String syntax = syntaxAndPattern.substring(0, colonIndex);
        final String pattern = syntaxAndPattern.substring(colonIndex + 1);
        return switch (syntax.toLowerCase()) {
            case "glob" -> path -> path.getFileSystem().equals(this)
                    && path.toString().matches(globToRegex(pattern));
            case "regex" -> path -> path.getFileSystem().equals(this)
                    && path.toString().matches(pattern);
            default -> throw new UnsupportedOperationException("Syntax not supported: " + syntax);
        };
    }

    private @NonNull String globToRegex(final @NonNull String glob) {
        final StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*' -> {
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        sb.append(".*");
                        i++;
                    } else {
                        sb.append("[^/]*");
                    }
                }
                case '?' -> sb.append("[^/]");
                case '.' -> sb.append("\\.");
                case '{' -> sb.append("(?:");
                case '}' -> sb.append(")");
                case ',' -> sb.append("|");
                default -> sb.append(c);
            }
        }
        sb.append("$");
        return sb.toString();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("UserPrincipalLookupService is not supported for HTTP file systems");
    }

    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException("WatchService is not supported for HTTP file systems");
    }
}