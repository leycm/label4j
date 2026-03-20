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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.NonNull;

import java.io.StringReader;
import java.net.URI;
import java.util.*;

public interface FileParser {

    @NonNull String extension();

    @NonNull Map<String, String> parse(@NonNull URI uri) throws Exception;


    private static @NonNull Map<String, String> flattenRaw(final @NonNull Map<String, Object> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
        }
        return result;
    }


    final class Json implements FileParser {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public @NonNull String extension() {
            return "json";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            return flattenRaw(mapper.readValue(content, new TypeReference<>() {}));
        }
    }

    final class Yaml implements FileParser {
        private final YAMLMapper mapper = new YAMLMapper();

        @Override
        public @NonNull String extension() {
            return "yml";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            return flattenRaw(mapper.readValue(content, new TypeReference<>() {}));
        }
    }

    final class Toml implements FileParser {
        private final TomlMapper mapper = new TomlMapper();

        @Override
        public @NonNull String extension() {
            return "toml";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            return flattenRaw(mapper.readValue(content, new TypeReference<>() {}));
        }
    }

    final class Property implements FileParser {

        @Override
        public @NonNull String extension() {
            return "properties";
        }

        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            Properties props = new Properties();
            props.load(new StringReader(content));
            Map<String, String> result = new LinkedHashMap<>();
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
            return result;
        }
    }
}