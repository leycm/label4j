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

import java.util.Properties;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

/**
 * Strategy interface for reading a localization file and returning its
 * contents as a flat {@code Map<String, String>}.
 *
 * <p>Each implementation handles one file format (JSON, YAML, TOML, or
 * {@code .properties}) and is identified by its {@link #extension()}. The
 * concrete classes {@link Json}, {@link Yaml}, {@link Toml}, and
 * {@link Property} are nested inside this interface for namespace
 * cohesion.</p>
 *
 * <p>Files are read via {@link FileUtils}, which abstracts over the
 * underlying storage scheme (classpath resource, local file system, or
 * remote URL). The raw Jackson/Properties map is flattened to
 * {@code String} values by converting each value through
 * {@link String#valueOf(Object)}.</p>
 *
 * <p>Thread Safety: All built-in implementations are effectively
 * immutable and therefore safe for concurrent use.</p>
 *
 * @since 1.0
 * @see FileUtils
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface FileParser {

    /**
     * Returns the file extension (without leading dot) handled by this
     * parser, for example {@code "json"} or {@code "yml"}.
     *
     * @return the extension; never {@code null}
     */
    @NonNull String extension();

    /**
     * Reads the file at the given URI and returns a flat
     * {@code Map<String, String>} of all key-value pairs.
     *
     * <p>The exact exception type depends on the parser implementation
     * and the underlying IO operation. Callers must handle
     * {@link Exception} broadly.</p>
     *
     * @param uri the location of the file to parse;
     *            must not be {@code null}
     * @return a map of translation entries; never {@code null},
     *         may be empty
     * @throws Exception if the file cannot be read or the content is
     *                   malformed for this parser's format
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    @NonNull Map<String, String> parse(@NonNull URI uri) throws Exception;

    /**
     * Converts a raw {@code Map<String, Object>} (as returned by
     * Jackson mappers) into a {@code Map<String, String>} by calling
     * {@link String#valueOf(Object)} on each value.
     *
     * <p>{@code null} values in {@code raw} are preserved as
     * {@code null} in the result.</p>
     *
     * @param raw the raw map from Jackson; must not be {@code null}
     * @return a flat string map; never {@code null}
     */
    private static @NonNull Map<String, String> flattenRaw(
            final @NonNull Map<String, Object> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(),
                    entry.getValue() == null ? null : entry.getValue().toString());
        }
        return result;
    }

    /**
     * {@link FileParser} implementation for JSON files.
     *
     * <p>Uses Jackson's {@link ObjectMapper} to parse the file content,
     * then flattens all top-level values to strings. Only flat
     * (non-nested) JSON objects are supported.</p>
     *
     * @since 1.0
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    final class Json implements FileParser {

        // reusable Jackson mapper — ObjectMapper is thread-safe after configuration
        private final ObjectMapper mapper = new ObjectMapper();

        /**
         * Returns {@code "json"}.
         *
         * @return the extension; never {@code null}
         */
        @Override
        public @NonNull String extension() {
            return "json";
        }

        /**
         * Reads and parses a JSON file.
         *
         * @param uri the location of the JSON file; must not be {@code null}
         * @return a flat map of key-value pairs; never {@code null}
         * @throws Exception if the file cannot be read or the JSON is
         *                   malformed
         * @throws NullPointerException if {@code uri} is {@code null}
         */
        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            return flattenRaw(mapper.readValue(content, new TypeReference<>() {}));
        }
    }

    /**
     * {@link FileParser} implementation for YAML files ({@code .yml}).
     *
     * <p>Uses Jackson's {@link YAMLMapper} to parse the file content.
     * Only flat (non-nested) YAML mappings are supported.</p>
     *
     * @since 1.0
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    final class Yaml implements FileParser {

        // reusable Jackson YAML mapper — thread-safe after configuration
        private final YAMLMapper mapper = new YAMLMapper();

        /**
         * Returns {@code "yml"}.
         *
         * @return the extension; never {@code null}
         */
        @Override
        public @NonNull String extension() {
            return "yml";
        }

        /**
         * Reads and parses a YAML file.
         *
         * @param uri the location of the YAML file; must not be {@code null}
         * @return a flat map of key-value pairs; never {@code null}
         * @throws Exception if the file cannot be read or the YAML is
         *                   malformed
         * @throws NullPointerException if {@code uri} is {@code null}
         */
        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            return flattenRaw(mapper.readValue(content, new TypeReference<>() {}));
        }
    }

    /**
     * {@link FileParser} implementation for TOML files.
     *
     * <p>Uses Jackson's {@link TomlMapper} to parse the file content.
     * Only flat (non-nested) TOML tables are supported.</p>
     *
     * @since 1.0
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    final class Toml implements FileParser {

        // reusable Jackson TOML mapper — thread-safe after configuration
        private final TomlMapper mapper = new TomlMapper();

        /**
         * Returns {@code "toml"}.
         *
         * @return the extension; never {@code null}
         */
        @Override
        public @NonNull String extension() {
            return "toml";
        }

        /**
         * Reads and parses a TOML file.
         *
         * @param uri the location of the TOML file; must not be {@code null}
         * @return a flat map of key-value pairs; never {@code null}
         * @throws Exception if the file cannot be read or the TOML is
         *                   malformed
         * @throws NullPointerException if {@code uri} is {@code null}
         */
        @Override
        public @NonNull Map<String, String> parse(final @NonNull URI uri) throws Exception {
            String content = FileUtils.readFile(uri);
            return flattenRaw(mapper.readValue(content, new TypeReference<>() {}));
        }
    }

    /**
     * {@link FileParser} implementation for Java {@code .properties} files.
     *
     * <p>Uses {@link Properties} to parse the file content.
     * All string property names and values are returned as-is without
     * any transformation.</p>
     *
     * @since 1.0
     * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
     */
    final class Property implements FileParser {

        /**
         * Returns {@code "properties"}.
         *
         * @return the extension; never {@code null}
         */
        @Override
        public @NonNull String extension() {
            return "properties";
        }

        /**
         * Reads and parses a {@code .properties} file.
         *
         * @param uri the location of the properties file;
         *            must not be {@code null}
         * @return a map of key-value pairs; never {@code null}
         * @throws Exception if the file cannot be read or is malformed
         * @throws NullPointerException if {@code uri} is {@code null}
         */
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