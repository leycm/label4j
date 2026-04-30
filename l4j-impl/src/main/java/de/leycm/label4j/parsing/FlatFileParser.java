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
package de.leycm.label4j.parsing;

import com.electronwill.nightconfig.json.JsonParser;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.yaml.YamlParser;
import de.leycm.label4j.exception.FlatParseException;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public interface FlatFileParser{

    static @NonNull FlatFileParser json(final @NonNull String extension) {
        return new FlatNightParser(new JsonParser(), extension);
    }

    static @NonNull FlatFileParser json() {
        return new FlatNightParser(new JsonParser(), "json");
    }

    static @NonNull FlatFileParser yaml(final @NonNull String extension) {
        return new FlatNightParser(new YamlParser(), extension);
    }

    static @NonNull FlatFileParser yaml() {
        return new FlatNightParser(new YamlParser(), "yaml");
    }

    static @NonNull FlatFileParser yml() {
        return new FlatNightParser(new YamlParser(), "yml");
    }

    static @NonNull FlatFileParser toml(final @NonNull String extension) {
        return new FlatNightParser(new TomlParser(), extension);
    }

    static @NonNull FlatFileParser toml() {
        return new FlatNightParser(new TomlParser(), "toml");
    }

    static @NonNull FlatFileParser properties(final @NonNull String extension) {
        return new FlatPropertyParser(extension);
    }

    static @NonNull FlatFileParser properties() {
        return new FlatPropertyParser("properties");
    }

    @NonNull String getExtension();

    default @NonNull String getEnding() {
        return "." + getExtension();
    }

    @NonNull Map<String, String> parse(@NonNull Path file) throws FlatParseException;

    // ==== Helper Methods ====================================================

    // Converts a raw {@code Map<String, Object>} into a
    // {@code Map<String, String>} by calling {@link String#valueOf(Object)}
    // on each value.
    // {@code null} values in {@code raw} are preserved as
    // {@code null} in the result.
    static @NonNull Map<String, String> flattenRaw(
            final @NonNull Map<String, Object> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(),
                    entry.getValue() == null ? null : entry.getValue().toString());
        }
        return result;
    }

    // Helper to read file content
    static @NonNull String readFile(final @NonNull Path file) throws IOException {
        return Files.readString(file);
    }

}