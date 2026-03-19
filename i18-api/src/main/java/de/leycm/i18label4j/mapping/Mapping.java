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
package de.leycm.i18label4j.mapping;

import lombok.NonNull;

import java.util.function.Supplier;

public record Mapping(@NonNull String key,
                      @NonNull Supplier<Object> value
) {

    public String valueAsString() {
        return String.valueOf(value.get());
    }

    @Override
    public String toString() {
        return Mapping.class.getSimpleName() + "{" +
                "key=\"" + key + '\"' +
                ", value=" + value.get() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Mapping mapping = (Mapping) obj;
        return key.equals(mapping.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

}
