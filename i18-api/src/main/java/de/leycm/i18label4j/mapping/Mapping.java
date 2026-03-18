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

import java.util.Set;
import java.util.function.Supplier;

public record Mapping(@NonNull MappingRule rule,
                      @NonNull String key,
                      @NonNull Supplier<String> value
) {

    public static @NonNull String apply(final @NonNull Set<Mapping> mappings,
                                        final @NonNull String text) {
        if (mappings.isEmpty()) return text;

        String result = text;

        for (final Mapping mapping : mappings)
            result = mapping.apply(result);

        return result;
    }

    public @NonNull String apply(final @NonNull String text) {
        final var matcher = rule.getPattern().matcher(text);
        final var result = new StringBuilder(text.length());

        int lastEnd = 0;
        while (matcher.find()) {
            if (!matcher.group(1).equals(key)) continue;
            result.append(text, lastEnd, matcher.start());
            result.append(value.get());
            lastEnd = matcher.end();
        }

        if (lastEnd == 0) return text;

        result.append(text, lastEnd, text.length());
        return result.toString();
    }

}
