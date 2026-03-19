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

import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class MappingRule {

    /** Double Curly style: {@code {{variable}}} (Vue, Handlebars, Jinja) */
    public static final @NonNull MappingRule DOUBLE_CURLY = new MappingRule("{{", "}}");

    /** Dollar Curly style: {@code ${variable}} (ES6, Kotlin, Bash-Strings) */
    public static final @NonNull MappingRule DOLLAR_CURLY = new MappingRule("${", "}");

    /** Section style: {@code §{variable}} (Custom/Legacy) */
    public static final @NonNull MappingRule SECTION_CURLY = new MappingRule("§{", "}");

    /** Tag style: {@code <variable>} (XML/HTML-ish) */
    public static final @NonNull MappingRule TAG = new MappingRule("<", ">");

    /** Curly style: {@code {variable}} (MessageFormat, Python f-strings) */
    public static final @NonNull MappingRule CURLY = new MappingRule("{", "}");

    /** Percent style: {@code %variable%} (Windows Environment, Batch) */
    public static final @NonNull MappingRule PERCENT = new MappingRule("%", "%");

    /** Bracket style: {@code [variable]} (BBCode, Wiki-Syntax) */
    public static final @NonNull MappingRule BRACKET = new MappingRule("[", "]");

    /** Shell style: {@code $variable} (Unix Shell, PHP) */
    public static final @NonNull MappingRule SHELL = new MappingRule("$", "");

    /** Format String style: {@code %variable} (C-style, String.format) */
    public static final @NonNull MappingRule FORMAT_STRING = new MappingRule("%", "");

    /** MiniMessage style: {@code <var:variable>} (Adventure/Kyori) */
    public static final @NonNull MappingRule MINI_MESSAGE = new MappingRule("<var:", ">");

    /** Minecraft Legacy style: {@code §:variable} */
    public static final @NonNull MappingRule MINECRAFT_LEGACY = new MappingRule("§:", "");

    private static final String ESCAPED_PREFIX = "\u0000P";
    private static final String ESCAPED_SUFFIX = "\u0000S";

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

    public String apply(final @NonNull String s, final @NonNull Set<Mapping> mappings) {
        if (mappings.isEmpty()) return s;

        String result = s
                .replace("\\" + prefix, ESCAPED_PREFIX)
                .replace("\\" + suffix, ESCAPED_SUFFIX);

        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            final String key = matcher.group(1);

            String replacement = null;

            for (final Mapping mapping : mappings) {
                if (!mapping.key().equals(key)) continue;
                replacement = mapping.valueAsString();
                break;
            }

            final String finalReplacement = Objects
                    .requireNonNullElse(replacement, matcher.group(0));

            matcher.appendReplacement(sb, finalReplacement);
        }

        matcher.appendTail(sb);
        result = sb.toString();

        result = result
                .replace(ESCAPED_PREFIX, prefix)
                .replace(ESCAPED_SUFFIX, suffix);

        return result;
    }

    @Override
    public String toString() {
        return MappingRule.class.getSimpleName() + "@" + prefix + "variable" + suffix;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MappingRule that = (MappingRule) obj;
        return prefix.equals(that.prefix) && suffix.equals(that.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, suffix);
    }

}
