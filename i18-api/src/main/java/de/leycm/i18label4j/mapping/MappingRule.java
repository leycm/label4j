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

import lombok.Getter;
import lombok.NonNull;

import java.util.regex.Pattern;

@Getter
public class MappingRule {

    /**
     * Dollar-style placeholder pattern: {@code ${variable}}
     */
    public static final @NonNull MappingRule DOLLAR = new MappingRule("${", "}");

    /**
     * Percent-style placeholder pattern: {@code %variable%}
     */
    public static final @NonNull MappingRule PERCENT = new MappingRule("%", "%");

    /**
     * F-string style placeholder pattern: {@code %variable}
     */
    public static final @NonNull MappingRule FSTRING = new MappingRule("%", "");

    /**
     * Curly brace placeholder pattern: {@code {{variable}}}
     */
    public static final @NonNull MappingRule CURLY = new MappingRule("{{", "}}");

    /**
     * MiniMessage style placeholder pattern: {@code <var:variable>}
     */
    public static final @NonNull MappingRule MINI_MESSAGE = new MappingRule("<var:", ">");

    private final String prefix;
    private final String suffix;
    private final Pattern pattern;

    public MappingRule(final @NonNull String prefix, final @NonNull String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;

        if (suffix.isEmpty()) {
            this.pattern = Pattern.compile(Pattern.quote(prefix) + "([A-Za-z0-9_]+)");
        } else {
            this.pattern = Pattern.compile(Pattern.quote(prefix)
                    + "([^" + Pattern.quote(suffix.substring(0, 1)) + "]+)"
                    + Pattern.quote(suffix));
        }
    }

}
