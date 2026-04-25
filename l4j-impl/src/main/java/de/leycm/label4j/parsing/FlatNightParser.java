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
package de.leycm.label4j.parsing;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ConfigParser;
import de.leycm.label4j.exception.FlatParseException;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class FlatNightParser implements FlatFileParser {
    private final @NonNull ConfigParser<? extends Config> parser;
    private final @NonNull String extension;

    public FlatNightParser(
            final @NonNull  ConfigParser<? extends  Config> parser,
            final @NonNull String extension
    ) {
        this.parser = parser;
        this.extension = extension;
    }

    @Override
    public @NonNull String getExtension() {
        return extension;
    }

    @Override
    public @NonNull Map<String, String> parse(@NonNull Path file) throws FlatParseException {
        try {
            final String content = FlatFileParser.readFile(file);
            final Config config = parser.parse(content);
            final Map<String, Object> raw = config.entrySet().stream()
                    .collect(Collectors.toMap(Config.Entry::getKey, Config.Entry::getValue));
            return FlatFileParser.flattenRaw(raw);
        } catch (IOException e) {
            throw new FlatParseException("Failed to read file: " + file, e);
        } catch (Exception e) {
            throw new FlatParseException("Invalid " + extension + " format in file: " + file, e);
        }
    }
}
