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
package de.leycm.i18label4j.source;

import de.leycm.i18label4j.file.FileParser;
import de.leycm.i18label4j.file.FileUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

import java.net.URI;
import java.util.*;

/**
 * A {@link LocalizationSource} that reads localization files from a
 * directory tree where each locale occupies its own sub-directory.
 *
 * <p>The expected layout is:</p>
 * <pre>
 * &lt;directory&gt;/
 *   en/
 *     messages.json
 *     errors.json
 *   de/
 *     messages.json
 *     errors.json
 * </pre>
 *
 * <p>Locale directories are named using IETF BCP 47 language tags with
 * underscores as separators (e.g. {@code en_US}). When reading a locale
 * the parser scans all files inside the locale subdirectory that match
 * the configured extension. Each file's stem (filename without extension)
 * is prepended to the key names found inside it, producing fully qualified
 * keys such as {@code messages.greeting}.</p>
 *
 * <p>Only flat file structures are supported — subdirectories inside a
 * locale directory are silently skipped.</p>
 *
 * <p>Thread Safety: Instances are effectively immutable after construction
 * and may be shared across threads safely.</p>
 *
 * @since 1.0.0
 * @see LocalizationSource
 * @see FileSource
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
@RequiredArgsConstructor
public final class DirSource implements LocalizationSource {

    private final @NonNull URI directory;
    private final @NonNull FileParser parser;

    /**
     * Discovers all locale subdirectories under the configured root
     * directory and returns a set of the corresponding {@link Locale}s.
     *
     * <p>Each direct child entry of {@link #directory} that is itself a
     * directory is treated as a locale identifier. The entry name is
     * converted from underscore notation to BCP 47 (e.g.
     * {@code en_US} → {@code en-US}) before being parsed. Entries that
     * do not produce a valid locale are silently ignored.</p>
     *
     * @return a set of discovered locales; never {@code null}, may be empty
     */
    @Override
    public @NonNull Set<Locale> getLocalizations() {
        Set<Locale> locales = new HashSet<>();
        if (!FileUtils.isDir(directory)) return locales;

        for (URI entry : FileUtils.readDir(directory)) {
            if (!FileUtils.isDir(entry)) continue;
            String name = lastName(entry);
            try {
                locales.add(Locale.forLanguageTag(name.replace("_", "-")));
            } catch (Exception ignored) {}
        }
        return locales;
    }

    /**
     * Loads all translation files for the given locale from its
     * subdirectory and returns a combined flat map.
     *
     * <p>The locale's language tag is converted back to underscore
     * notation ({@code en-US} → {@code en_US}) to locate the
     * corresponding directory. Each file matching the parser's extension
     * is read and its entries are prefixed with the file stem and a dot.
     * For example, a file named {@code messages.json} containing
     * {@code {"greeting": "Hello"}} contributes the entry
     * {@code messages.greeting = Hello}.</p>
     *
     * @param locale the locale to load; must not be {@code null}
     * @return a flat map of fully qualified key-to-value pairs;
     *         never {@code null}, may be empty
     * @throws NoSuchElementException if no subdirectory exists for
     *                                the requested locale
     * @throws Exception              if any file cannot be read or
     *                                parsed
     */
    @Override
    public @NonNull Map<String, String> getLocalization(final @NonNull Locale locale)
            throws Exception {
        String tag = locale.toLanguageTag().replace("-", "_");
        URI localeDir = resolve(directory, tag);

        if (!FileUtils.isDir(localeDir)) {
            throw new NoSuchElementException("No directory for locale: " + locale);
        }

        String ext = "." + parser.extension();
        Map<String, String> result = new LinkedHashMap<>();

        for (URI entry : FileUtils.readDir(localeDir)) {
            // note: skip sub-directories — only flat structure supported
            if (!FileUtils.isFile(entry)) continue;
            String name = lastName(entry);
            if (!name.endsWith(ext)) continue;

            String key = name.substring(0, name.length() - ext.length());
            Map<String, String> parsed = parser.parse(entry);
            for (Map.Entry<String, String> e : parsed.entrySet()) {
                result.put(key + "." + e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Creates a {@link DirSource} that parses JSON files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link DirSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull DirSource json(final @NonNull URI directory) {
        return new DirSource(directory, new FileParser.Json());
    }

    /**
     * Creates a {@link DirSource} that parses YAML files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link DirSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull DirSource yaml(final @NonNull URI directory) {
        return new DirSource(directory, new FileParser.Yaml());
    }

    /**
     * Creates a {@link DirSource} that parses TOML files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link DirSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull DirSource toml(final @NonNull URI directory) {
        return new DirSource(directory, new FileParser.Toml());
    }

    /**
     * Creates a {@link DirSource} that parses {@code .properties} files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link DirSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull DirSource properties(final @NonNull URI directory) {
        return new DirSource(directory, new FileParser.Property());
    }

    /**
     * Extracts the last path segment from a URI, stripping any trailing
     * slash first.
     *
     * @param uri the URI to extract the last segment from;
     *            must not be {@code null}
     * @return the last segment; never {@code null}
     */
    private static @NonNull String lastName(final @NonNull URI uri) {
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
    private static @NonNull URI resolve(final @NonNull URI base,
                                        final @NonNull String name) {
        String s = base.toString();
        if (!s.endsWith("/")) s += "/";
        return URI.create(s + name);
    }
}