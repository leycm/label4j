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
package de.leycm.i18label4j.label;

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.LabelProvider;
import de.leycm.i18label4j.mapping.Mapping;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Function;

public class LocaleLabel implements Label {
    private final @NonNull LabelProvider provider;
    private final @NonNull Set<Mapping> mappings;
    private final @NonNull String key;
    private final @NonNull Function<Locale, String> fallback;

    public LocaleLabel(final @NonNull LabelProvider provider, final @NonNull String key,
                       final @NonNull Function<Locale, String> fallback) {
        this(provider, new HashSet<>(), key, fallback);
    }

    @ApiStatus.Internal
    public LocaleLabel(final @NonNull LabelProvider provider,
                       final @NonNull Set<Mapping> mappings,
                       final @NonNull String key,
                       final @NonNull Function<Locale, String> fallback) {
        this.provider = provider;
        this.mappings = mappings;
        this.key = key;
        this.fallback = fallback;
    }

    @Override
    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    @Override
    public @NonNull Set<Mapping> getMappings() {
        return Collections.unmodifiableSet(mappings);
    }

    public @NonNull String getKey() {
        return key;
    }

    @Override
    public @NonNull Label mapTo(@NonNull Mapping mapping) {
        mappings.add(mapping);
        return this;
    }

    @Override
    public @NonNull String in(@NonNull Locale locale) {
        return provider.translate(key, locale, fallback);
    }

    @Override
    public @NonNull String toString() {
        try {
            return serialize(String.class);
        } catch (Throwable e) {
            return LiteralLabel.class.getSimpleName() +
                    "@" + Integer.toHexString(hashCode());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LocaleLabel that = (LocaleLabel) obj;
        return provider.equals(that.getProvider()) &&
                key.equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, key);
    }


}
