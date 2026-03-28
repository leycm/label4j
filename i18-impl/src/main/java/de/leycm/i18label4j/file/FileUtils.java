/*
 * This file is part of the i18label4j Library.
 *
 * Licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0)
 * You should have received a copy of the license in LICENSE.LGPL
 * If not, see https://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Copyright 2026 (c) leycm <leycm@proton.me>
 * Copyright 2026 (c) maintainers
 */
package de.leycm.i18label4j.file;

import lombok.NonNull;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static utility class for reading files and directories from multiple
 * URI schemes.
 *
 * <p>{@link FileUtils} abstracts over three URI schemes so that
 * localization files can be loaded transparently from the JVM classpath
 * ({@code resource://}), the local file system ({@code file://}), or a
 * remote HTTP/HTTPS server ({@code http://} / {@code https://}).
 * All public methods dispatch on the scheme of the supplied {@link URI}
 * and delegate to the appropriate private scheme-specific implementation.</p>
 *
 * <p>For remote directories the implementation expects a {@code .dir}
 * listing file whose lines each name one child entry (file or directory).
 * This convention allows directory enumeration without server-side
 * directory listing support.</p>
 *
 * <p>Thread Safety: All methods are stateless and therefore thread-safe.</p>
 *
 * @since 1.0
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public final class FileUtils {

    // ==== Constants =========================================================

    // Filename of the remote directory listing file
    private static final String LS_FILE = ".dir";

    /** Utility class */
    private FileUtils() {
        throw new UnsupportedOperationException(getClass().getName() + " is a static only class and cannot be instantiated");
    }

    // ==== Public Helper API =================================================

    /** Converts a {@link Locale} to a file tag string suitable for naming
     * localization files, using underscores as separators and lower-case
     * letters (e.g. {@code en}, {@code en_us}, {@code de}). This is the
     * inverse of {@link #fromFileTag(String)}.
     *
     * @param locale the locale to convert; must not be {@code null}
     * @return the file tag string; never {@code null}
     */
    public static String toFileTag(final @NonNull Locale locale) {
        return locale.toLanguageTag().replace("-", "_").toLowerCase();
    }

    /** Converts a file tag string (e.g. {@code en}, {@code en_us}, {@code de})
     * back to a {@link Locale}. Underscores are treated as separators and
     * converted to hyphens before parsing the tag. This is the inverse of
     * {@link #toFileTag(Locale)}.
     *
     * @param tag the file tag string; must not be {@code null}
     * @return the corresponding {@link Locale}; never {@code null}
     */
    public static Locale fromFileTag(final @NonNull String tag) {
        return Locale.forLanguageTag(tag.replace("_", "-"));
    }

    /**
     * Extracts the last path segment from a URI, stripping any trailing
     * slash first.
     *
     * @param uri the URI to extract the last segment from;
     *            must not be {@code null}
     * @return the last segment; never {@code null}
     */
    public static @NonNull String lastName(final @NonNull URI uri) {
        String path = uri.toString();
        // note: strip trailing slash if present
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    /**
     * Resolves a child name relative to a base URI, ensuring a
     * trailing slash before appending.
     *
     * @param base the base directory URI; must not be {@code null}
     * @param name the child name to append; must not be {@code null}
     * @return the resolved child URI; never {@code null}
     */
    public static @NonNull URI resolve(final @NonNull URI base,
                                       final @NonNull String name) {
        String s = base.toString();
        if (!s.endsWith("/")) s += "/";
        return URI.create(s + name);
    }

    // ==== File Resolve API ==================================================

    /**
     * Returns the set of child {@link URI}s contained in the directory
     * at {@code uri}.
     *
     * <p>The returned URIs reference both files and subdirectories
     * directly inside the given directory — no recursive traversal is
     * performed.</p>
     *
     * @param uri the directory URI; must not be {@code null}
     * @return a set of child URIs; never {@code null}, may be empty
     * @throws IllegalArgumentException if the URI scheme is not
     *         {@code resource}, {@code file}, {@code http}, or
     *         {@code https}; or if the URI does not point to a directory
     * @throws RuntimeException if listing the directory fails for any
     *                          IO or JAR-access reason
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public static @NonNull Set<URI> readDir(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> readDirResource(uri);
            case "http", "https" -> readDirRemote(uri);
            case "file" -> readDirFile(uri);
            case null -> readDirFile(uri); // treat scheme-less URIs as file paths
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    /**
     * Reads and returns the full text content of the file at {@code uri}.
     *
     * @param uri the file URI; must not be {@code null}
     * @return the file contents as a string; never {@code null}
     * @throws IllegalArgumentException if the URI scheme is unsupported
     * @throws RuntimeException         if reading the file fails
     * @throws NullPointerException     if {@code uri} is {@code null}
     */
    public static @NonNull String readFile(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> readFileResource(uri);
            case "http", "https" -> readFileRemote(uri);
            case "file" -> readFileFile(uri);
            case null -> readFileFile(uri); // treat scheme-less URIs as file paths
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    /**
     * Returns {@code true} if the given URI points to a directory.
     *
     * @param uri the URI to test; must not be {@code null}
     * @return {@code true} if the URI is a directory; {@code false} otherwise
     * @throws IllegalArgumentException if the URI scheme is unsupported
     * @throws NullPointerException     if {@code uri} is {@code null}
     */
    public static boolean isDir(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> isDirResource(uri);
            case "http", "https" -> isDirRemote(uri);
            case "file" -> isDirFile(uri);
            case null -> isDirFile(uri); // treat scheme-less URIs as file paths
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    /**
     * Returns {@code true} if the given URI points to a regular file.
     *
     * @param uri the URI to test; must not be {@code null}
     * @return {@code true} if the URI is a file; {@code false} otherwise
     * @throws IllegalArgumentException if the URI scheme is unsupported
     * @throws NullPointerException     if {@code uri} is {@code null}
     */
    public static boolean isFile(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> isFileResource(uri);
            case "http", "https" -> isFileRemote(uri);
            case "file" -> isFileFile(uri);
            case null -> isFileFile(uri);
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    // ==== Resource Scheme Implementation ====================================

    /**
     * Lists child entries inside a classpath resource directory.
     *
     * <p>Supports both exploded directories (running from the file system)
     * and JAR entries (running from a packaged JAR). In the JAR case the
     * entry names are scanned for direct children of the given path.</p>
     *
     * @param uri the {@code resource://} URI; must not be {@code null}
     * @return a set of child URIs; never {@code null}
     * @throws IllegalArgumentException if the resource does not exist or
     *                                  is not a directory
     * @throws RuntimeException         if listing fails
     */
    private static @NonNull Set<URI> readDirResource(final @NonNull URI uri) {
        String path = resourcePath(uri);
        URL url = FileUtils.class.getClassLoader().getResource(path);
        if (url == null) throw new IllegalArgumentException("Resource directory does not exist: " + uri);

        try {
            if ("file".equals(url.getProtocol())) {
                Path dirPath = Path.of(url.toURI());
                if (!Files.isDirectory(dirPath)) {
                    throw new IllegalArgumentException("Resource is a file, not a directory: " + uri);
                }
                try (Stream<Path> paths = Files.list(dirPath)) {
                    return paths
                            .map(p -> createResourceUri(path + "/" + p.getFileName()))
                            .collect(Collectors.toSet());
                }

            } else if ("jar".equals(url.getProtocol())) {
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                String prefix = path.endsWith("/") ? path : path + "/";
                Set<URI> children = new HashSet<>();

                try (JarFile jar = new JarFile(jarPath)) {
                    jar.stream()
                            .map(JarEntry::getName)
                            .filter(name -> name.startsWith(prefix) && !name.equals(prefix))
                            .map(name -> name.substring(prefix.length()).split("/")[0])
                            .distinct()
                            .filter(name -> !name.isEmpty())
                            .forEach(name -> children.add(createResourceUri(prefix + name)));
                }
                return children;
            }

            throw new IllegalArgumentException("Unsupported resource protocol: " + url.getProtocol());
        } catch (Exception e) {
            throw new RuntimeException("Failed to list resource directory: " + uri, e);
        }
    }

    // ==== Remote Scheme Implementation ======================================

    /**
     * Lists child entries inside a remote directory using the {@code .dir}
     * listing convention.
     *
     * <p>Fetches the {@code .dir} file from the remote directory URI and
     * interprets each non-empty line as the name of a child entry.</p>
     *
     * @param uri the remote directory URI; must not be {@code null}
     * @return a set of child URIs; never {@code null}
     * @throws RuntimeException if the {@code .dir} listing cannot be read
     */
    private static @NonNull Set<URI> readDirRemote(final @NonNull URI uri) {
        String base = ensureTrailingSlash(uri.toString());
        String lsContent = readFileRemote(URI.create(base + LS_FILE));
        Set<URI> children = new HashSet<>();

        for (String line : lsContent.split("\n")) {
            String name = line.trim();
            if (!name.isEmpty()) {
                children.add(URI.create(base + name));
            }
        }
        return children;
    }

    // ==== File Scheme Implementation ========================================

    /**
     * Lists child entries inside a local file-system directory.
     *
     * @param uri the {@code file://} URI; must not be {@code null}
     * @return a set of child URIs; never {@code null}
     * @throws IllegalArgumentException if the path does not exist or
     *                                  is not a directory
     * @throws RuntimeException         if listing fails with an
     *                                  {@link IOException}
     */
    private static @NonNull Set<URI> readDirFile(final @NonNull URI uri) {
        Path path = toLocalPath(uri);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Directory does not exist: " + uri);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("URI is a file, not a directory: " + uri);
        }

        try {
            try (Stream<Path> paths = Files.list(path)) {
                return paths.map(Path::toUri)
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + uri, e);
        }
    }

    // ==== File Reading Implementations ======================================

    /**
     * Reads a classpath resource file and returns its content as a string.
     *
     * @param uri the {@code resource://} URI; must not be {@code null}
     * @return the file content; never {@code null}
     * @throws IllegalArgumentException if the resource is not found
     * @throws RuntimeException         if reading fails
     */
    private static @NonNull String readFileResource(final @NonNull URI uri) {
        String path = resourcePath(uri);
        InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(path);

        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    /**
     * Fetches a remote file over HTTP/HTTPS and returns its content
     * as a string.
     *
     * @param uri the remote file URI; must not be {@code null}
     * @return the file content; never {@code null}
     * @throws RuntimeException if the HTTP connection or reading fails
     */
    private static @NonNull String readFileRemote(final @NonNull URI uri) {
        try (InputStream is = uri.toURL().openStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read remote file: " + uri, e);
        }
    }

    /**
     * Reads a local file and returns its content as a string.
     *
     * @param uri the {@code file://} URI; must not be {@code null}
     * @return the file content; never {@code null}
     * @throws RuntimeException if reading fails with an {@link IOException}
     */
    private static @NonNull String readFileFile(final @NonNull URI uri) {
        try {
            return Files.readString(toLocalPath(uri), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + uri, e);
        }
    }

    // ==== Type Detection Implementations ===================================

    /**
     * Returns {@code true} if the given {@code resource://} URI points
     * to a directory inside the classpath.
     *
     * @param uri the {@code resource://} URI; must not be {@code null}
     * @return {@code true} if the URI resolves to a directory
     */
    private static boolean isDirResource(final @NonNull URI uri) {
        String path = resourcePath(uri);
        URL url = FileUtils.class.getClassLoader().getResource(path);

        if (url == null) return false;

        if ("file".equals(url.getProtocol())) {
            try {
                return Files.isDirectory(Path.of(url.toURI()));
            } catch (Exception e) {
                return false;
            }
        } else if ("jar".equals(url.getProtocol())) {
            String jarInternalPath = path.endsWith("/") ? path : path + "/";
            return FileUtils.class.getClassLoader().getResource(jarInternalPath) != null;
        }

        return false;
    }

    /**
     * Returns {@code true} if the given remote URI points to a directory
     * (detected by the presence of a {@code .dir} listing file).
     *
     * @param uri the remote URI; must not be {@code null}
     * @return {@code true} if a {@code .dir} file is accessible at the URI
     */
    private static boolean isDirRemote(final @NonNull URI uri) {
        String base = ensureTrailingSlash(uri.toString());
        try {
            readFileRemote(URI.create(base + LS_FILE));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the local path resolves to a directory.
     *
     * @param uri the {@code file://} URI; must not be {@code null}
     * @return {@code true} if the path is a directory
     */
    private static boolean isDirFile(final @NonNull URI uri) {
        return Files.isDirectory(toLocalPath(uri));
    }

    /**
     * Returns {@code true} if the local path resolves to a regular file.
     *
     * @param uri the {@code file://} URI; must not be {@code null}
     * @return {@code true} if the path is a regular file
     */
    private static boolean isFileFile(final @NonNull URI uri) {
        Path path = toLocalPath(uri);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    /**
     * Returns {@code true} if the remote URI resolves to a readable file
     * (i.e. is reachable but is not a directory).
     *
     * @param uri the remote URI; must not be {@code null}
     * @return {@code true} if the URI is a readable file
     */
    private static boolean isFileRemote(final @NonNull URI uri) {
        try {
            if (isDirRemote(uri)) { return false; }
            readFileRemote(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the {@code resource://} URI resolves to a
     * classpath file (not a directory).
     *
     * @param uri the {@code resource://} URI; must not be {@code null}
     * @return {@code true} if the resource is a regular file
     */
    private static boolean isFileResource(final @NonNull URI uri) {
        String path = resourcePath(uri);
        URL url = FileUtils.class.getClassLoader().getResource(path);

        if (url == null) return false;

        if ("file".equals(url.getProtocol())) {
            try {
                Path p = Path.of(url.toURI());
                return Files.exists(p) && Files.isRegularFile(p);
            } catch (Exception e) {
                return false;
            }
        } else if ("jar".equals(url.getProtocol())) {
            return !isDirResource(uri);
        }

        return false;
    }

    // ==== Utility Methods ===================================================

    /**
     * Extracts the classpath-relative path from a {@code resource://} URI.
     *
     * <p>Strips leading slashes so the path is suitable for
     * {@link ClassLoader#getResourceAsStream(String)}.</p>
     *
     * @param uri the {@code resource://} URI; must not be {@code null}
     * @return the cleaned path string; never {@code null}
     */
    private static @NonNull String resourcePath(final @NonNull URI uri) {
        StringBuilder path = new StringBuilder();

        if (uri.getHost() != null) {
            path.append(uri.getHost());
        }

        if (uri.getPath() != null) {
            path.append(uri.getPath());
        }

        String result = path.toString();

        while (result.startsWith("/")) {
            result = result.substring(1);
        }

        return result;
    }

    /**
     * Creates a {@code resource://} URI from the given classpath path.
     *
     * <p>Leading slashes are stripped before constructing the URI.</p>
     *
     * @param path the classpath path; must not be {@code null}
     * @return the constructed {@code resource://} URI; never {@code null}
     */
    private static @NonNull URI createResourceUri(final @NonNull String path) {
        String cleaned = path.replaceAll("^/+", "");
        return URI.create("resource://" + cleaned);
    }

    /**
     * Converts a {@code file://} or scheme-less {@link URI} to a
     * {@link Path}.
     *
     * <p>If the URI has no scheme, only its path component is used to
     * create the {@link Path}.</p>
     *
     * @param uri the URI to convert; must not be {@code null}
     * @return the corresponding {@link Path}; never {@code null}
     */
    private static @NonNull Path toLocalPath(final @NonNull URI uri) {
        if (uri.getScheme() == null) {
            return Paths.get(uri.getPath());
        }
        return Paths.get(uri);
    }

    /**
     * Ensures the string representation of {@code uri} ends with a
     * trailing slash.
     *
     * @param uri the URI string to normalize; must not be {@code null}
     * @return the URI string with a trailing slash; never {@code null}
     */
    private static @NonNull String ensureTrailingSlash(final @NonNull String uri) {
        return uri.endsWith("/") ? uri : uri + "/";
    }
}