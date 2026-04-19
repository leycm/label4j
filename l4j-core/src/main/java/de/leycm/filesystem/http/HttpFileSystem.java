package de.leycm.filesystem.http;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class HttpFileSystem extends FileSystem {
    private final @NonNull HttpFileSystemProvider provider;
    private final @NonNull URI baseUri;
    private final @NonNull String indexFile;
    private final @NonNull HttpClient httpClient;
    private final @NonNull AtomicBoolean closed = new AtomicBoolean(false);

    public HttpFileSystem(
            final @NonNull HttpFileSystemProvider provider,
            final @NonNull URI baseUri,
            final @NonNull String indexFile) {
        this.provider = provider;
        this.baseUri = baseUri;
        this.indexFile = indexFile;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
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
        return true;
    }

    @Override
    public @NonNull String getSeparator() {
        return "/";
    }

    @Override
    public @NonNull Iterable<Path> getRootDirectories() {
        return Collections.singleton(getPath("/"));
    }

    @Override
    public @NonNull Iterable<FileStore> getFileStores() {
        return Collections.emptySet();
    }

    @Override
    public @NonNull Set<String> supportedFileAttributeViews() {
        return Set.of("basic");
    }

    @Override
    public @NonNull Path getPath(
            final @NonNull String first,
            final @NonNull String @NonNull ... more
    ) {
        final StringBuilder sb = new StringBuilder(first);
        for (final String part : more) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.isEmpty() || sb.charAt(sb.length() - 1) != '/') {
                sb.append('/');
            }
            sb.append(part);
        }
        String pathStr = sb.toString();
        if (!pathStr.startsWith("/")) {
            pathStr = "/" + pathStr;
        }
        return new HttpPath(this, normalizePath(pathStr));
    }

    public @NonNull URI getBaseUri() {
        return baseUri;
    }

    public @NonNull String getIndexFile() {
        return indexFile;
    }

    public @NonNull HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public @NonNull PathMatcher getPathMatcher(
            final @NonNull String syntaxAndPattern
    ) {
        if (syntaxAndPattern.startsWith("glob:")) {
            final String pattern = syntaxAndPattern.substring(5);
            return new GlobPathMatcher(pattern);
        } else if (syntaxAndPattern.startsWith("regex:")) {
            final String pattern = syntaxAndPattern.substring(6);
            return path -> path.toString().matches(pattern);
        } else {
            throw new IllegalArgumentException("Unsupported syntax: " + syntaxAndPattern);
        }
    }

    private record GlobPathMatcher(@NonNull Pattern regexPattern) implements PathMatcher {

        private GlobPathMatcher(final @NonNull String globPattern) {
            this(Pattern.compile(globToRegex(globPattern)));
        }

        @Override
        public boolean matches(final @NonNull Path path) {
            return regexPattern.matcher(path.toString()).matches();
        }

        private static @NonNull String globToRegex(final @NonNull String glob) {
            final StringBuilder sb = new StringBuilder("^");
            for (int i = 0; i < glob.length(); i++) {
                final char c = glob.charAt(i);
                switch (c) {
                    case '*':
                        if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                            sb.append(".*");
                            i++;
                        } else {
                            sb.append("[^/]*");
                        }
                        break;
                    case '?':
                        sb.append("[^/]");
                        break;
                    case '.':
                    case '+':
                    case '(':
                    case ')':
                    case '|':
                    case '^':
                    case '$':
                    case '{':
                    case '}':
                    case '[':
                    case ']':
                        sb.append('\\').append(c);
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case '\\':
                        if (i + 1 < glob.length()) {
                            sb.append(Pattern.quote(String.valueOf(glob.charAt(++i))));
                        } else {
                            sb.append('\\');
                        }
                        break;
                    default:
                        sb.append(c);
                }
            }
            sb.append('$');
            return sb.toString();
        }
    }

    @Override
    public @NonNull UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("UserPrincipalLookupService is not supported for HTTPFileSystem");
    }

    @Override
    public @NonNull WatchService newWatchService() throws IOException {
        throw new IOException(new UnsupportedOperationException("WatchService is not supported for HTTPFileSystem"));
    }

    private @NonNull String normalizePath(final @NonNull String path) {
        return Paths.get(path).normalize().toString().replace('\\', '/');
    }
}