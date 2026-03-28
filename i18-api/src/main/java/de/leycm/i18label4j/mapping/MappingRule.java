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

import java.util.*;

/**
 * Defines the placeholder syntax used to substitute {@link Mapping}
 * values into a source string.
 *
 * <p>A {@link MappingRule} is constructed from a prefix and an optional
 * suffix that delimit placeholder tokens. For example,
 * {@link #DOLLAR_CURLY} matches {@code ${key}}, while
 * {@link #PERCENT} matches {@code %key%}.</p>
 *
 * <p>The {@link #apply(String, Set)} method scans the input character by
 * character, identifying placeholder tokens by their surrounding delimiters
 * and substituting them with values from the supplied {@link Set} of
 * {@link Mapping} instances. Unrecognised tokens are left unchanged.</p>
 *
 * <p>Escape sequences: a backslash immediately before the prefix — are
 * resolved inline: the backslash is consumed and the prefix is emitted
 * literally, preventing the token from being treated as a placeholder.</p>
 *
 * <p>When a suffix is present, dot ({@code .}) is permitted inside key
 * names (e.g. {@code ${user.name}}), enabling nested or namespaced keys.
 * Without a suffix, key characters are limited to {@code A–Z}, {@code a–z},
 * {@code 0–9}, underscore and hyphen, because the token boundary would
 * otherwise be ambiguous.</p>
 *
 * <p>Thread Safety: Instances are effectively immutable after construction
 * and may be shared freely across threads.</p>
 *
 * @since 1.0
 * @see Mapping
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public class MappingRule {

    // ==== Built-in Rules ====================================================

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

    /** Shell like style: {@code $variable} (Unix Shell, PHP) */
    public static final @NonNull MappingRule SHELL = new MappingRule("$", "");

    /** Format String style: {@code %variable} (C-style, String.format) */
    public static final @NonNull MappingRule FORMAT_STRING = new MappingRule("%", "");

    /** MiniMessage style: {@code <var:variable>} (Adventure/Kyori) */
    public static final @NonNull MappingRule MINI_MESSAGE = new MappingRule("<var:", ">");

    /** Minecraft Legacy style: {@code §:variable} */
    public static final @NonNull MappingRule MINECRAFT_LEGACY = new MappingRule("§:", "");

    // ==== Internal Constants ================================================

    // maximum allowed input length (1 MB) passed to apply()
    private static final int INPUT_LIMIT = 1_000_000;
    // maximum number of substitutions performed in a single apply() call
    private static final int PLACEHOLDER_LIMIT = 10_000;
    // maximum allowed prefix and suffix length
    private static final int PREFIX_LIMIT = 5;

    private final @NonNull String prefix;
    private final @NonNull String suffix;
    private final boolean hasSuffix;

    // ==== Public API =======================================================

    /**
     * Constructs a new {@link MappingRule} with the given delimiter pair.
     *
     * <p>The prefix marks where a placeholder token begins; the suffix marks
     * where it ends. If the suffix is empty, the token extends to the last
     * consecutive valid key character instead (e.g. {@link #SHELL}:
     * {@code $key}).</p>
     *
     * @param prefix the opening delimiter; never {@code null}
     * @param suffix the closing delimiter; never {@code null},
     *               may be empty to indicate an open-ended token boundary
     * @throws IllegalArgumentException if {@code prefix} is empty
     * @throws IllegalArgumentException if a prefix or suffix exceeds
     *                                  {@link #PREFIX_LIMIT} characters
     * @throws NullPointerException if {@code prefix} or {@code suffix}
     *                              is {@code null}
     */
    public MappingRule(final @NonNull String prefix, final @NonNull String suffix) {
        this.prefix    = prefix;
        this.suffix    = suffix;
        this.hasSuffix = !suffix.isEmpty();

        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be empty");
        }

        if (prefix.length() > PREFIX_LIMIT) {
            throw new IllegalArgumentException("prefix must not exceed " + PREFIX_LIMIT + " characters");
        }

        if (suffix.length() > PREFIX_LIMIT) {
            throw new IllegalArgumentException("suffix must not exceed " + PREFIX_LIMIT + " characters");
        }
    }

    /**
     * Replaces all placeholder tokens in {@code input} with the values
     * from the supplied {@link Set} of {@link Mapping} instances.
     *
     * <p>The input is scanned character by character. When the prefix is
     * detected, the key is extracted either up to the next occurrence of the
     * suffix (when one is configured) or up to the last consecutive valid
     * key character (when no suffix is configured). If the extracted key is
     * present in {@code mappings}, the entire token — including delimiters —
     * is replaced by the mapped value. Unknown keys are emitted unchanged,
     * including their surrounding delimiters.</p>
     *
     * <p>Escape sequences: a backslash immediately before the prefix — are
     * resolved inline: the backslash is consumed and the prefix characters
     * are appended literally to the output, with no further token matching
     * attempted at that position.</p>
     *
     * <p>Two safety limits are enforced: inputs longer than {@code 1 000 000}
     * characters are rejected with an {@link IllegalArgumentException}, and
     * the method returns the input unchanged without scanning when
     * {@code mappings} is empty or {@code input} does not contain the
     * prefix string.</p>
     *
     * @param input    the source text to process; never {@code null}
     * @param mappings the set of key-value substitutions to apply;
     *                 never {@code null}
     * @return the substituted string; never {@code null}. Returns
     *         {@code input} unchanged when no tokens are found or
     *         {@code mappings} is empty.
     * @throws IllegalArgumentException if {@code input} exceeds
     *                                  {@link #INPUT_LIMIT} characters
     * @throws IllegalArgumentException if more than {@link #PLACEHOLDER_LIMIT}
     *                                  placeholder registered
     * @throws NullPointerException     if {@code input} or {@code mappings}
     *                                  is {@code null}
     */
    public @NonNull String apply(final @NonNull String input,
                                 final @NonNull Set<Mapping> mappings) {
        if (input.length() > INPUT_LIMIT) throw new IllegalArgumentException("Input too large " + INPUT_LIMIT);
        if (input.isEmpty() || mappings.isEmpty()) return input;
        if (!input.contains(prefix)) return input;


        final Map<String, String> lookup = buildLookup(mappings);
        final int   len    = input.length();
        final int   pLen   = prefix.length();
        final int   sLen   = suffix.length();
        final StringBuilder sb = new StringBuilder(len + 32);

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

            if (hasSuffix) {
                // suffix: key extends to next occurrence of suffix
                int suffixPos = input.indexOf(suffix, keyStart);
                if (suffixPos < 0) {
                    sb.append(input.charAt(i++));
                    continue;
                }
                keyEnd = suffixPos;

                // note: validate key characters skip replace if any char is illegal
                boolean validKey = true;
                for (int k = keyStart; k < keyEnd; k++) {
                    if (!isKeyChar(input.charAt(k))) {
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
                while (keyEnd < len && isKeyChar(input.charAt(keyEnd))) keyEnd++;
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
                if (hasSuffix) sb.append(suffix);
            }

            i = keyEnd + (hasSuffix ? sLen : 0);
        }

        return sb.toString();
    }

    // ==== Internal Implementation ===========================================

    // Returns {@code true} if {@code s} contains {@code sub} starting at
    // {@code from}, without throwing when {@code from} is out of bounds.
    private boolean startsWith(final String s, final String sub, final int from) {
        if (from < 0 || from + sub.length() > s.length()) return false;
        return s.startsWith(sub, from);
    }

    // Returns {@code true} if {@code c} is a valid placeholder key character.
    // Allowed: A-Z, a-z, 0-9, underscore, hyphen.
    // Dot is only allowed when a suffix is present (e.g. ${user.name}),
    // since without a suffix the token boundary would be ambiguous.
    private boolean isKeyChar(final char c) {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '-'
                || (c == '.' && hasSuffix);
    }

    // Builds a flat {@link Map} from key to string value for fast O(1)
    // lookup during the replacement loop in {@link #apply(String, Set)}.
    // For single-entry sets an immutable singleton map is returned to
    // avoid an unnecessary {@link HashMap} allocation.
    private @NonNull Map<String, String> buildLookup(
            final @NonNull Set<Mapping> mappings) {
        if (mappings.size() == 1) {
            Mapping m = mappings.iterator().next();
            return Map.of(m.key(), m.valueAsString());
        }
        Map<String, String> map = new HashMap<>((int)(mappings.size() / 0.75f) + 1);
        for (Mapping m : mappings) map.put(m.key(), m.valueAsString());
        return map;
    }

    // ==== Object Methods ===================================================

    /**
     * Returns a human-readable representation of this rule's syntax,
     * for example {@code MappingRule@${variable}}.
     *
     * @return a non-{@code null} debug string
     */
    @Override
    public @NonNull String toString() {
        return MappingRule.class.getSimpleName() + "@" + prefix + "variable" + suffix;
    }

    /**
     * Determines equality based on the {@code prefix} and {@code suffix} pair.
     *
     * @param obj the object to compare; can be {@code null}
     * @return {@code true} if {@code obj} is a {@link MappingRule} with
     *         the same prefix and suffix
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MappingRule that = (MappingRule) obj;
        return prefix.equals(that.prefix) && suffix.equals(that.suffix);
    }

    /**
     * Returns a hash code derived from both the {@code prefix} and
     * {@code suffix} fields, consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(prefix, suffix);
    }
}