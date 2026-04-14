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
package de.leycm.label4j.placeholder;

import de.leycm.init4j.instance.Instanceable;
import de.leycm.label4j.LabelProvider;

import lombok.NonNull;

import java.util.Objects;
import java.util.function.Supplier;

public record Placeholder(
        @NonNull String key,
        @NonNull Supplier<Object> value
) implements Comparable<Placeholder> {

    public Placeholder {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Placeholder key must not be empty");
        }

        for (char c : key.toCharArray()) {
            if (PlaceholderRule.isKeyChar(c, false)) {
                throw new IllegalArgumentException("Placeholder key contains illegal character: " + c);
            }
        }
    }

    public @NonNull Object get() {
        return value.get();
    }

    public @NonNull String getAsString() {
        return String.valueOf(value.get());
    }

    @Override
    public int compareTo(final @NonNull Placeholder other) {
        return this.key.compareTo(other.key);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Placeholder that = (Placeholder) object;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public @NonNull String toString() {
        if (!Instanceable.hasInstance(LabelProvider.class)) {
            return toString(PlaceholderRule.DOLLAR_CURLY);
        }
        // todo: replace with return toString(LabelProvider.getInstance().placeholderRule());
        return "";
    }

    public @NonNull String toString(final @NonNull PlaceholderRule rule) {
        return rule.prefix() + key + rule.suffix().orElse("");
    }
}
