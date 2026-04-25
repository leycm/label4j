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

import lombok.NonNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("ClassCanBeRecord")
final class PropertyParser implements FileParser {

    private final @NonNull String extension;

    PropertyParser(final @NonNull String extension) {
        this.extension = extension;
    }

    @Override
    public @NonNull String getExtension() {
        return extension;
    }

    @Override
    public @NonNull Map<String, String> parse(final @NonNull Path file) throws IOException {
        final String content = FileParser.readFile(file);
        final Properties props = new Properties();
        props.load(new StringReader(content));

        final Map<String, String> result = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }

        return result;
    }
}
