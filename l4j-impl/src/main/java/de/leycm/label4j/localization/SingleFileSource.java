/*
 * Copyright (C) 2026 leycm <leycm@proton.me>
 *
 * This file is part of label4j.
 *
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
package de.leycm.label4j.localization;

import de.leycm.label4j.parsing.FileParser;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SingleFileSource implements LocalizationSource{

    // the path to read the files from
    private final @NonNull Path directory;
    // the parser for reading translation files
    private final @NonNull FileParser parser;

    SingleFileSource(
            final @NonNull Path directory,
            final @NonNull FileParser parser
    ) {
        this.directory = directory;
        this.parser = parser;
    }

    @Override
    public @NonNull Set<Locale> getLocalizations() {
        try (var stream = Files.walk(directory, 1)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(parser.getExtension()))
                    .map(name -> name.substring(0, name.length()
                            - parser.getExtension().length()))
                    .map(FileParser::filenameToLocale)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            return Set.of();
        }
    }

    @Override
    public @Unmodifiable @NonNull Map<String, Localization> getLocalization(
            @NonNull Locale locale
    ) throws Exception {
        final String tag = FileParser.localeToFilename(locale);
        final String filename = tag + parser.getExtension();
        final Path file = directory.resolve(filename);
        final Map<String, String> parse = parser.parse(file);

        final Map<String, Localization> result = new HashMap<>();
        for (Map.Entry<String, String> entry : parse.entrySet()) {
            result.put(
                    entry.getKey(),
                    Localization.of(entry.getKey(), locale, entry.getValue())
            );
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean containsLocalization(@NonNull Locale locale) {
        final String tag = FileParser.localeToFilename(locale);
        final String filename = tag + parser.getExtension();
        final Path file = directory.resolve(filename);

        // note: this check fits better for cases where there are no listing available,
        //       e.g. when the directory is on a remote server or in a jar file
        return Files.exists(file) && Files.isRegularFile(file);
    }

}
