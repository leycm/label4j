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
package de.leycm.label4j;

import de.leycm.label4j.placeholder.Placeholder;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LabelFactory<T> {
    private final Map<String, Function<T, Object>> objectResolvers = new ConcurrentHashMap<>();
    private final Set<Placeholder> placeholders = new HashSet<>();

    private final LabelProvider provider;

    @ApiStatus.Internal
    LabelFactory(final @NonNull LabelProvider provider) {
        this.provider = provider;
    }

    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    public @NonNull Label create(String key, T t) {
        return null;
    }

    public @NonNull Label createLiteral(String literal, T t) {
        return null;
    }

}
