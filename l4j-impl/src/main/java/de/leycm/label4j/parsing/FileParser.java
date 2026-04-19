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

import com.google.gson.JsonParseException;
import com.moandjiezana.toml.Toml;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import lombok.NonNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public interface FileParser {

    static @NonNull String localeToFilename(final @NonNull Locale locale) {
        return locale.getLanguage().toLowerCase() + "_" + locale.getCountry().toLowerCase();
    }

    static @NonNull Locale filenameToLocale(final @NonNull String filename) {
        return Locale.forLanguageTag(filename.replace("_", "-"));
    }

    @NonNull String getExtension();

    @NonNull Map<String, String> parse(@NonNull Path file) throws Exception;

    // ==== Helper Methods ====================================================

    // Converts a raw {@code Map<String, Object>} into a
    // {@code Map<String, String>} by calling {@link String#valueOf(Object)}
    // on each value.
    // {@code null} values in {@code raw} are preserved as
    // {@code null} in the result.
    private static @NonNull Map<String, String> flattenRaw(
            final @NonNull Map<String, Object> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(),
                    entry.getValue() == null ? null : entry.getValue().toString());
        }
        return result;
    }

    // Helper to read file content
    private static @NonNull String readFile(final @NonNull Path file) throws IOException {
        return Files.readString(file);
    }

    // ==== Implementations ==================================================

    final class Json implements FileParser {

        @Override
        public @NonNull String getExtension() {
            return "json";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull Path file) throws JsonParseException {
            try {
                final String content = Files.readString(file);
                final JSONObject obj = new JSONObject(content);
                return flattenRaw(obj.toMap());
            } catch (IOException e) {
                throw new JsonParseException("Failed to read file: " + file, e);
            } catch (JSONException e) {
                throw new JsonParseException("Invalid JSON format in file: " + file, e);
            }
        }
    }

    final class YamlParser implements FileParser {

        // local SnakeYAML instance - not thread-safe
        private final Yaml yaml = new Yaml();

        public YamlParser() { }

        @Override
        public @NonNull String getExtension() {
            return "yml";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull Path file) throws YAMLException {
            try {
                final String content = readFile(file);
                final Map<String, Object> raw = yaml.load(content);
                return flattenRaw(raw == null ? Collections.emptyMap() : raw);
            } catch (IOException e) {
                throw new YAMLException("Failed to read file: " + file, e);
            }
        }
    }

    final class TomlParser implements FileParser {

        // local toml4j instance - thread-safe
        private final Toml toml = new Toml();

        public TomlParser() { }

        @Override
        public @NonNull String getExtension() {
            return "toml";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull Path file) throws IllegalArgumentException {
            try {
                final String content = readFile(file);
                final Map<String, Object> raw = toml.read(content).toMap();
                return flattenRaw(raw);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read file: " + file, e);
            }
        }
    }

    final class PropertyParser implements FileParser {

        public PropertyParser() { }

        @Override
        public @NonNull String getExtension() {
            return "properties";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull Path file) throws IOException {
            final String content = readFile(file);
            final Properties props = new Properties();
            props.load(new StringReader(content));

            final Map<String, String> result = new LinkedHashMap<>();
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }

            return result;
        }
    }

}