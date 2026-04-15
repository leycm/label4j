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
package de.leycm.label4j.localization;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

public record Localization(
        @NonNull String key,
        @NonNull Locale origin,
        @NonNull Locale request,
        @Nullable String result
)  {

    public static final @NonNull String WARMUP_KEY = "__warmup__";
    public static final @NonNull String LITERAL_KEY = "__literal__";

    // ==== Static Factory Methods ============================================

    @ApiStatus.Internal
    public static @NonNull Localization empty(
            @NonNull String key,
            @NonNull Locale locale
    ) {
        return new Localization(key, locale, locale, null);
    }

    public static @NonNull Localization of(
            @NonNull String key,
            @NonNull Locale locale,
            @NonNull String result
    ) {
        return new Localization(key, locale, locale, result);
    }

    public static @NonNull Localization of(
            @NonNull String key,
            @NonNull Locale origin,
            @NonNull Locale request,
            @NonNull String result
    ) {
        return new Localization(key, origin, request, result);
    }

    // ==== Localization Validation ===========================================

    public Localization {
        if (result != null && result.isBlank()) {
            throw new IllegalArgumentException("The result of a Localization cannot be blank");
        }
    }

    // ==== Getter Methods ====================================================

    @Override
    public @NonNull Locale origin() {
        return origin;
    }

    @Override
    public @NonNull Locale request() {
        return request;
    }

    @Override
    @Deprecated
    @ApiStatus.Internal
    public @Nullable String result() {
        return result;
    }

    // ==== State Methods ====================================================

    public boolean isFulfilled() {
        return origin.equals(request);
    }

    public boolean isEmpty() {
        return result == null;
    }

    public boolean isPresent() {
        return !isEmpty();
    }

    // ==== Convert and Filter Methods ========================================

    public @NonNull Localization toFulfilled() {
        if (isFulfilled()) return this;
        // note: result will be striped if it came from an other origin
        return new Localization(key, request, request, null);
    }

    // ==== Getters ===========================================================

    public @NonNull String get() {
        return orElseThrow();
    }

    public @NonNull String orElse(final @NonNull String fallback) {
        if (isEmpty()) return fallback;
        return result;
    }

    public @NonNull String orElseGet(final @NonNull Supplier<@NonNull String> supplier) {
        if (isEmpty()) return supplier.get();
        return result;
    }

    public @NonNull String orElseThrow() {
        if (isEmpty()) throw new NoSuchElementException(
                "No result of Localization present");
        return result;
    }

    public <X extends Throwable> @NonNull String orElseThrow(
            final @NonNull Supplier<@NonNull X> supplier
    ) throws X {
        if (isEmpty()) throw supplier.get();
        return result;
    }

    // ==== Object Methods ===================================================

    @Override
    public @NonNull String toString() {
        return "Localization{" +
                "key='" + key + '\'' +
                ", origin=" + origin +
                ", request=" + request +
                ", result='" + result + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Localization that = (Localization) object;
        return Objects.equals(key, that.key)
                && Objects.equals(origin, that.origin)
                && Objects.equals(result, that.result)
                && Objects.equals(request, that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, origin, request, result);
    }
}
