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
import lombok.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SuppressWarnings("ClassCanBeRecord")
final class JsonParser implements FileParser {
    private final @NonNull String extension;

    JsonParser(@NonNull String extension) {
        this.extension = extension;
    }

    @Override
    public @NonNull String getExtension() {
        return extension;
    }

    @Override
    public @NonNull Map<String, String> parse(final @NonNull Path file) throws JsonParseException {
        try {
            final String content = Files.readString(file);
            final JSONObject obj = new JSONObject(content);
            return FileParser.flattenRaw(obj.toMap());
        } catch (IOException e) {
            throw new JsonParseException("Failed to read file: " + file, e);
        } catch (JSONException e) {
            throw new JsonParseException("Invalid JSON format in file: " + file, e);
        }
    }
}