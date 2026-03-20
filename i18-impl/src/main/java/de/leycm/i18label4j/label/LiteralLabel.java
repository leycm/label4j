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

@SuppressWarnings("ClassCanBeRecord") // cause: mutable mappings
public class LiteralLabel implements Label {
    private final LabelProvider provider;
    private final Set<Mapping> mappings;
    private final String literal;

    public LiteralLabel(final @NonNull LabelProvider provider,
                        final @NonNull String literal) {
        this(provider, new HashSet<>(), literal);
    }

    @ApiStatus.Internal
    public LiteralLabel(final @NonNull LabelProvider provider,
                        final @NonNull Set<Mapping> mappings,
                        final @NonNull String literal) {
        this.provider = provider;
        this.mappings = mappings;
        this.literal = literal;
    }

    @Override
    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    @Override
    public @NonNull Set<Mapping> getMappings() {
        return Collections.unmodifiableSet(mappings);
    }

    public @NonNull String getLiteral() {
        return literal;
    }

    @Override
    public @NonNull Label mapTo(final @NonNull Mapping mapping) throws IllegalArgumentException {
        if (mappings.contains(mapping)) throw new IllegalArgumentException("Mapping with key \"" + mapping.key() + "\" already exists for this label.");
        mappings.add(mapping);
        return this;
    }

    @Override
    public @NonNull String in(final @NonNull Locale locale) {
        return literal;
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
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LiteralLabel that = (LiteralLabel) obj;
        return provider.equals(that.getProvider()) &&
                literal.equals(that.getLiteral());
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, literal);
    }

}
