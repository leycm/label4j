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

import de.leycm.label4j.exception.DuplicatePlaceholderException;
import de.leycm.label4j.label.LiteralLabel;
import de.leycm.label4j.label.LocaleLabel;
import de.leycm.label4j.placeholder.Placeholder;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class LabelFactory<T> {
    private final @NonNull Map<String, Function<T, @Nullable Object>> objectResolvers;
    private final @NonNull Set<Placeholder> resolvers;

    private final @NonNull LabelProvider provider;

    @ApiStatus.Internal
    LabelFactory(final @NonNull LabelProvider provider) {
        this.objectResolvers = new ConcurrentHashMap<>();
        this.resolvers = ConcurrentHashMap.newKeySet();
        this.provider = provider;
    }

    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    // ==== Replacement Methods ===============================================

    public @NonNull LabelFactory<T> addReplacement(
            final @NonNull String key,
            final @NonNull Object value
    ) throws DuplicatePlaceholderException {
        return addReplacement(key, () -> value);
    }

    public @NonNull LabelFactory<T> addReplacement(
            final @NonNull String key,
            final @NonNull Supplier<@Nullable Object> supplier
    ) throws DuplicatePlaceholderException {
        return addReplacement(new Placeholder(key, supplier));
    }

    public @NonNull LabelFactory<T> addReplacement(
            final @NonNull String key,
            final @NonNull Function<T, @Nullable Object> function
    ) throws DuplicatePlaceholderException {
        if(this.resolvers.contains(new Placeholder(key, () -> null))
                || this.objectResolvers.containsKey(key)) {
            throw new DuplicatePlaceholderException(key);
        }

        this.objectResolvers.put(key, function);
        return this;
    }

    public @NonNull LabelFactory<T> addReplacement(
            @NonNull Placeholder @NonNull ... placeholders
    ) throws DuplicatePlaceholderException {

        for (final Placeholder placeholder : placeholders) {
            if(this.resolvers.contains(placeholder)
                    || this.objectResolvers.containsKey(placeholder.key())) {
                throw new DuplicatePlaceholderException(placeholder.key());
            }
            this.resolvers.add(placeholder);
        }

        return this;
    }

    public @NonNull Label create(String key, T t) {
        return applyPlaceholder(new LocaleLabel(key, provider), t);
    }

    public @NonNull Label createLiteral(String literal, T t) {
        return applyPlaceholder(new LiteralLabel(literal, provider), t);
    }


    private @NonNull Label applyPlaceholder(
            final @NonNull Label label,
            final @NonNull T t
    ) {
        label.replace(resolvers);

        for (final Map.Entry<String, Function<T, @Nullable Object>> entry : objectResolvers.entrySet()) {
            label.replace(entry.getKey(), () -> entry.getValue().apply(t));
        }
        return label;
    }
}
