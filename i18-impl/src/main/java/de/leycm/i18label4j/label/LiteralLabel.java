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

import java.util.*;

public record LiteralLabel(
        @NonNull LabelProvider provider,
        @NonNull Set<Mapping> mappings,
        @NonNull String literal
) implements Label {

    public LiteralLabel { }

    public LiteralLabel(@NonNull LabelProvider provider,
                        @NonNull String literal) {
        this(provider, new HashSet<>(), literal);
    }

    @Override
    public @NonNull Set<Mapping> mappings() {
        return Collections.unmodifiableSet(mappings);
    }

    @Override
    public @NonNull Label mapTo(@NonNull Mapping mapping) {
        mappings.add(mapping);
        return this;
    }

    @Override
    public @NonNull String in(@NonNull Locale locale) {
        return literal;
    }

    @Override
    public @NonNull String toString() {
        try {return serialize();
        } catch (Throwable e) {
            return Objects.toString(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LiteralLabel that = (LiteralLabel) obj;
        return provider.equals(that.provider()) &&
                literal.equals(that.literal());
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, literal);
    }
}
