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
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;

@RequiredArgsConstructor
public final class FileSource implements LocalizationSource {

    private final @NonNull URI directory;
    private final @NonNull FileParser parser;

    @Override
    public @NonNull Set<Locale> getLocalizations() {
        Set<Locale> locales = new HashSet<>();
        if (!FileUtils.isDir(directory)) return locales;

        String ext = "." + parser.extension();

        for (URI entry : FileUtils.readDir(directory)) {
            if (!FileUtils.isFile(entry)) continue;
            String name = lastName(entry);
            if (!name.endsWith(ext)) continue;
            String tag = name.substring(0, name.length() - ext.length());
            try {
                locales.add(Locale.forLanguageTag(tag.replace("_", "-")));
            } catch (Exception ignored) {}
        }
        return locales;
    }

    @Override
    public @NonNull Map<String, String> getLocalization(final @NonNull Locale locale) throws Exception {
        String tag = locale.toLanguageTag().replace("-", "_");
        URI file = resolve(directory, tag + "." + parser.extension());

        if (!FileUtils.isFile(file)) {
            throw new NoSuchElementException("No file for locale: " + locale);
        }
        return parser.parse(file);
    }

    @Contract("_ -> new")
    public static @NotNull FileSource json(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.Json());
    }

    @Contract("_ -> new")
    public static @NotNull FileSource yaml(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.Yaml());
    }

    @Contract("_ -> new")
    public static @NotNull FileSource toml(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.Toml());
    }

    @Contract("_ -> new")
    public static @NotNull FileSource properties(final @NonNull URI directory) {
        return new FileSource(directory, new FileParser.Property());
    }

    private static @NonNull String lastName(final @NonNull URI uri) {
        String path = uri.toString();
        // note: strip trailing slash if present
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static @NonNull URI resolve(final @NonNull URI base, final @NonNull String name) {
        String s = base.toString();
        if (!s.endsWith("/")) s += "/";
        return URI.create(s + name);
    }
}