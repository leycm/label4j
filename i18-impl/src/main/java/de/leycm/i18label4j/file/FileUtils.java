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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class FileUtils {

    private static final String LS_FILE = ".ls";

    private FileUtils() {}

    public static @NonNull Set<URI> readDir(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> readDirResource(uri);
            case "http", "https" -> readDirRemote(uri);
            case "file" -> readDirFile(uri);
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    public static @NonNull String readFile(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> readFileResource(uri);
            case "http", "https" -> readFileRemote(uri);
            case "file" -> readFileFile(uri);
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    public static boolean isDir(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> isDirResource(uri);
            case "http", "https" -> isDirRemote(uri);
            case "file" -> isDirFile(uri);
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }

    public static boolean isFile(final @NonNull URI uri) {
        return switch (uri.getScheme()) {
            case "resource" -> isFileResource(uri);
            case "http", "https" -> isFileRemote(uri);
            case "file" -> isFileFile(uri);
            default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
        };
    }


    private static Set<URI> readDirResource(URI uri) {
        String path = resourcePath(uri);
        URL url = FileUtils.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new IllegalArgumentException("Resource directory does not exist: " + uri);
        }

        try {
            if ("file".equals(url.getProtocol())) {
                Path dirPath = Path.of(url.toURI());
                if (!Files.isDirectory(dirPath)) {
                    throw new IllegalArgumentException("Resource is a file, not a directory: " + uri);
                }
                return Files.list(dirPath)
                        .map(p -> createResourceUri(path + "/" + p.getFileName()))
                        .collect(Collectors.toSet());

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

    private static Set<URI> readDirRemote(URI uri) {
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

    private static Set<URI> readDirFile(URI uri) {
        Path path = toLocalPath(uri);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Directory does not exist: " + uri);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("URI is a file, not a directory: " + uri);
        }

        try {
            return Files.list(path)
                    .map(Path::toUri)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + uri, e);
        }
    }

    private static String readFileResource(URI uri) {
        String path = resourcePath(uri);
        InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(path);

        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    private static String readFileRemote(URI uri) {
        try (InputStream is = uri.toURL().openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read remote file: " + uri, e);
        }
    }

    private static String readFileFile(URI uri) {
        try {
            return Files.readString(toLocalPath(uri), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + uri, e);
        }
    }

    private static boolean isDirResource(URI uri) {
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

    private static boolean isDirRemote(URI uri) {
        String base = ensureTrailingSlash(uri.toString());
        try {
            readFileRemote(URI.create(base + LS_FILE));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDirFile(URI uri) {
        return Files.isDirectory(toLocalPath(uri));
    }

    private static boolean isFileFile(URI uri) {
        Path path = toLocalPath(uri);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private static boolean isFileRemote(URI uri) {
        try {
            if (isDirRemote(uri)) {return false;}
            readFileRemote(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFileResource(URI uri) {
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

    private static String resourcePath(URI uri) {
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

    private static URI createResourceUri(String path) {
        path = path.replaceAll("^/+", "");
        return URI.create("resource://" + path);
    }

    private static Path toLocalPath(URI uri) {
        if (uri.getScheme() == null) {
            return Paths.get(uri.getPath());
        }
        return Paths.get(uri);
    }

    private static String ensureTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri : uri + "/";
    }
}