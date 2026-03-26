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
import org.jetbrains.annotations.Contract;

import java.net.URI;
import java.util.*;

/**
 * A {@link LocalizationSource} that reads one localization file per
 * locale from a flat directory.
 *
 * <p>The expected layout is:</p>
 * <pre>
 * &lt;directory&gt;/
 *   en.json
 *   en_US.json
 *   de.json
 * </pre>
 *
 * <p>Each file is named with the locale's IETF BCP 47 language tag
 * (underscores as separators, e.g. {@code en_US.json}) and contains all
 * translation keys for that locale. The keys inside the file are returned
 * as-is without any prefixing — unlike {@link DirSource}, which prepends
 * the file stem.</p>
 *
 * <p>Thread Safety: Instances are effectively immutable after construction
 * and may be shared across threads safely.</p>
 *
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 * @see LocalizationSource
 * @see DirSource
 * @since 1.0
 */
public final class FileSource implements LocalizationSource {

    // ==== Instance State ===================================================

    // the directory containing translation files
    private final @NonNull URI directory;
    // the parser for reading translation files
    private final @NonNull FileParser parser;

    /**
     * Creates a new {@link FileSource} with the given directory and parser.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @param parser    the file parser to use; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public FileSource(final @NonNull URI directory,
                      final @NonNull FileParser parser) {
        this.directory = directory;
        this.parser = parser;
    }

    // ==== Public API =========================================================

    /**
     * Scans the configured directory for files matching the parser's
     * extension and returns a set of the corresponding {@link Locale}s.
     *
     * <p>Each file whose name ends with the parser's extension is
     * interpreted as a locale identifier by stripping the extension and
     * converting underscores to hyphens (e.g. {@code en_US.json} →
     * {@code en-US}). Files that do not produce a valid locale are
     * silently ignored.</p>
     *
     * @return a set of discovered locales; never {@code null}, may be empty
     */
    @Override
    public @NonNull Set<Locale> getLocalizations() {
        Set<Locale> locales = new HashSet<>();
        if (!FileUtils.isDir(directory)) return locales;

        String ext = "." + parser.extension();

        for (URI entry : FileUtils.readDir(directory)) {
            if (!FileUtils.isFile(entry)) continue;
            String name = FileUtils.lastName(entry);
            if (!name.endsWith(ext)) continue;
            String tag = name.substring(0, name.length() - ext.length());
            try {
                locales.add(Locale.forLanguageTag(tag.replace("_", "-")));
            } catch (Exception ignored) {
            }
        }
        return locales;
    }

    /**
     * Loads the translation file for the given locale and returns its
     * contents as a flat map.
     *
     * <p>The locale's language tag is converted to underscore notation and
     * combined with the parser's extension to form the expected filename
     * (e.g. {@code en_US.json}). The file is then parsed by the
     * configured {@link FileParser} and its keys are returned unchanged.</p>
     *
     * @param locale the locale to load; must not be {@code null}
     * @return a flat map of key-to-value pairs; never {@code null},
     * may be empty
     * @throws NoSuchElementException if no file exists for the requested
     *                                locale
     * @throws Exception              if the file cannot be read or parsed
     */
    @Override
    public @NonNull Map<String, String> getLocalization(final @NonNull Locale locale)
            throws Exception {
        URI file = FileUtils.resolve(directory, FileUtils.toFileTag(locale) + "." + parser.extension());

        if (!FileUtils.isFile(file)) {
            throw new NoSuchElementException("No file for locale: " + locale + " (expected: " + file + ")");
        }
        return parser.parse(file);
    }

    // ==== Factory Methods ==================================================

    /**
     * Creates a {@link FileSource} that parses JSON files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link FileSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull FileSource json(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.Json());
    }

    /**
     * Creates a {@link FileSource} that parses YAML files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link FileSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull FileSource yaml(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.YamlParser());
    }

    /**
     * Creates a {@link FileSource} that parses TOML files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link FileSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull FileSource toml(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.TomlParser());
    }

    /**
     * Creates a {@link FileSource} that parses {@code .properties} files.
     *
     * @param directory the root directory URI; must not be {@code null}
     * @return a new {@link FileSource}; never {@code null}
     */
    @Contract("_ -> new")
    public static @NonNull FileSource properties(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.Property());
    }

}