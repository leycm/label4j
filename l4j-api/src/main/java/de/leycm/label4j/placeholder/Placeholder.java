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
package de.leycm.label4j.placeholder;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings("ClassCanBeRecord") // cause: equals/hashCode is intentionally key-only; record semantics would include value-supplier
public final class Placeholder implements Comparable<Placeholder> {

    // note: keep in mind that it is for EARLY checks and can be different from the actual check
    // see: PlaceholderRule#isKeyChar(char)
    private static final @NonNull Pattern EARLY_KEY_VALIDATOR = Pattern.compile("^[A-Za-z0-9_.-]+$");

    private final @NonNull String key;
    private final @NonNull Supplier<@Nullable Object> value;


    // ==== Placeholder Validation =============================================

    public Placeholder(@NonNull String key, @NonNull Supplier<@Nullable Object> value) {
        if (EARLY_KEY_VALIDATOR.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Placeholder key contains illegal characters. "
                            + EARLY_KEY_VALIDATOR.pattern()
                            + ", got: " + key
            );
        }
        this.key = key;
        this.value = value;
    }

    // ==== Getter Methods ====================================================

    public @NonNull String key() {
        return key;
    }

    public @Nullable Object get() {
        return value.get();
    }

    public @NonNull String getAsString() {
        // note: null Objects will be replaced with "null"
        return String.valueOf(get());
    }

    public @NonNull String getAsString(String fallback) {
        Object o = get();
        if (o == null) return fallback;
        return String.valueOf(o);
    }

    // ==== Object Methods ===================================================

    public @NonNull String toString(final @NonNull PlaceholderRule rule) {
        return rule.prefix() + key + rule.suffix().orElse("");
    }

    @Override
    public @NonNull String toString() {
            return toString(PlaceholderRule.DEFAULT);
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
    public int compareTo(final @NonNull Placeholder other) {
        return this.key.compareTo(other.key);
    }
}
