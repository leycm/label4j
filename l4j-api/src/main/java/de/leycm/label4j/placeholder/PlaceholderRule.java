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

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PlaceholderRule {

    // ==== Built-in Rules ====================================================

    /** Double Curly style: {@code {{variable}}} (Vue, Handlebars, Jinja) */
    public static final @NonNull PlaceholderRule DOUBLE_CURLY = new PlaceholderRule("{{", "}}");

    /** Dollar Curly style: {@code ${variable}} (ES6, Kotlin, Bash-Strings) */
    public static final @NonNull PlaceholderRule DOLLAR_CURLY = new PlaceholderRule("${", "}");

    /** Section style: {@code §{variable}} (Custom/Legacy) */
    public static final @NonNull PlaceholderRule SECTION_CURLY = new PlaceholderRule("§{", "}");

    /** Tag style: {@code <variable>} (XML/HTML-ish) */
    public static final @NonNull PlaceholderRule TAG = new PlaceholderRule("<", ">");

    /** Curly style: {@code {variable}} (MessageFormat, Python f-strings) */
    public static final @NonNull PlaceholderRule CURLY = new PlaceholderRule("{", "}");

    /** Percent style: {@code %variable%} (Windows Environment, Batch) */
    public static final @NonNull PlaceholderRule PERCENT = new PlaceholderRule("%", "%");

    /** Bracket style: {@code [variable]} (BBCode, Wiki-Syntax) */
    public static final @NonNull PlaceholderRule BRACKET = new PlaceholderRule("[", "]");

    /** Shell like style: {@code $variable} (Unix Shell, PHP) */
    public static final @NonNull PlaceholderRule SHELL = new PlaceholderRule("$", null);

    /** Format String style: {@code %variable} (C-style, String.format) */
    public static final @NonNull PlaceholderRule FORMAT_STRING = new PlaceholderRule("%", null);

    /** MiniMessage style: {@code <var:variable>} (Adventure/Kyori) */
    public static final @NonNull PlaceholderRule MINI_MESSAGE = new PlaceholderRule("<var:", ">");

    /** Minecraft Legacy style: {@code §:variable} */
    public static final @NonNull PlaceholderRule MINECRAFT_LEGACY = new PlaceholderRule("§:", "");

    // ==== Internal Constants ================================================

    // maximum allowed input length (1 MB) passed to apply()
    private static final int INPUT_LIMIT = 1_000_000;
    // maximum number of substitutions performed in a single apply() call
    private static final int PLACEHOLDER_LIMIT = 10_000;
    // maximum allowed prefix and suffix length
    private static final int PREFIX_LIMIT = 5;

    private final @NonNull String prefix;
    private final @Nullable String suffix;

    // ==== Placeholder Utils =================================================

    static boolean isKeyChar(final char c, final boolean hasSuffix) {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '-'
                || (c == '.' && hasSuffix);
    }

    // ==== Public API =======================================================

    public PlaceholderRule(final @NonNull String prefix, final @Nullable String suffix) {
        this.prefix = prefix;
        // note: converts "" to null
        this.suffix = suffix != null && suffix.isEmpty() ? null : suffix;


        // todo: add check for illegal chars in prefix or suffix

        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be empty");
        }

        if (prefix.length() > PREFIX_LIMIT) {
            throw new IllegalArgumentException("prefix must not exceed " + PREFIX_LIMIT + " characters");
        }

        if (suffix != null && suffix.length() > PREFIX_LIMIT) {
            throw new IllegalArgumentException("suffix must not exceed " + PREFIX_LIMIT + " characters");
        }
    }

    public @NonNull Optional<String> suffix() {
        return Optional.ofNullable(suffix);
    }

    public @NonNull String prefix() {
        return prefix;
    }

    public boolean hasSuffix() {
        return suffix != null;
    }

    public @NonNull String apply(final @NonNull String input,
                                 final @NonNull Set<Placeholder> placeholders) {
        if (input.length() > INPUT_LIMIT) throw new IllegalArgumentException("Input too large " + INPUT_LIMIT);
        if (input.isEmpty() || placeholders.isEmpty()) return input;
        if (!input.contains(prefix)) return input;

        final Map<String, String> lookup = buildLookup(placeholders);

        final String  sString   = Objects.requireNonNullElse(suffix, "");
        final boolean sPresence = !sString.isEmpty();
        final int     len       = input.length();
        final int     pLen      = prefix.length();
        final int     sLen      = sString.length();

        final StringBuilder sb  = new StringBuilder(len + 32);

        if (lookup.size() > PLACEHOLDER_LIMIT) {
            throw new IllegalArgumentException("too many mappings: " + lookup.size() +
                    " (maximum allowed is " + PLACEHOLDER_LIMIT + ")");
        }

        int i = 0;
        while (i < len) {

            if (input.charAt(i) == '\\' && startsWith(input, prefix, i + 1)) {
                // result: escape sequence: backslash immediately before prefix
                sb.append(prefix);
                i += 1 + pLen;
                continue;
            }

            if (!startsWith(input, prefix, i)) {
                // result: prefix not found at current position
                sb.append(input.charAt(i++));
                continue;
            }

            int keyStart = i + pLen;
            int keyEnd   = keyStart;

            if (suffix != null) {
                // suffix: key extends to next occurrence of suffix
                int suffixPos = input.indexOf(sString, keyStart);
                if (suffixPos < 0) {
                    sb.append(input.charAt(i++));
                    continue;
                }
                keyEnd = suffixPos;

                // note: validate key characters skip replace if any char is illegal
                boolean validKey = true;
                for (int k = keyStart; k < keyEnd; k++) {
                    if (!isKeyChar(input.charAt(k), sPresence)) {
                        validKey = false;
                        break;
                    }
                }

                if (!validKey) {
                    sb.append(input.charAt(i++));
                    continue;
                }
            } else {
                // no suffix: key extends to last consecutive valid key character
                while (keyEnd < len && isKeyChar(input.charAt(keyEnd), false)) keyEnd++;
            }

            if (keyEnd == keyStart) {
                // result: empty key, treat prefix as literal
                sb.append(input.charAt(i++));
                continue;
            }

            String key   = input.substring(keyStart, keyEnd);
            String value = lookup.get(key);

            if (value != null) {
                sb.append(value);
            } else {
                // unknown key, emit token unchanged
                sb.append(prefix).append(key);
                if (sPresence) sb.append(sString);
            }

            i = keyEnd + (sPresence ? sLen : 0);
        }

        return sb.toString();
    }

    // ==== Internal Implementation ===========================================

    private boolean startsWith(final String s, final String sub, final int from) {
        if (from < 0 || from + sub.length() > s.length()) return false;
        return s.startsWith(sub, from);
    }

    private @NonNull Map<String, String> buildLookup(
            final @NonNull Set<Placeholder> mappings) {
        if (mappings.size() == 1) {
            Placeholder p = mappings.iterator().next();
            return Map.of(p.key(), p.getAsString());
        }
        Map<String, String> map = new HashMap<>((int)(mappings.size() / 0.75f) + 1);
        for (Placeholder p : mappings) map.put(p.key(), p.getAsString());
        return map;
    }

    // ==== Object Methods ===================================================

    @Override
    public @NonNull String toString() {
        return PlaceholderRule.class.getSimpleName() + "@" + prefix + "variable" + suffix;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PlaceholderRule that = (PlaceholderRule) obj;
        return Objects.equals(prefix, that.prefix)
                && Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, suffix);
    }
}
