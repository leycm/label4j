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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines the placeholder syntax used to substitute {@link Mapping}
 * values into a source string.
 *
 * <p>A {@link MappingRule} is constructed from a prefix and an optional
 * suffix that delimit placeholder tokens. For example,
 * {@link #DOLLAR_CURLY} matches {@code ${key}}, while
 * {@link #PERCENT} matches {@code %key%}. At construction time the rule
 * compiles an optimized {@link Pattern} and pre-computes escape-sequence
 * metadata so that {@link #apply(String, Set)} can run efficiently at
 * runtime.</p>
 *
 * <p>The {@link #apply(String, Set)} method replaces all occurrences of
 * recognized placeholder tokens with their corresponding values from the
 * supplied {@link Set} of {@link Mapping} instances. Unrecognised tokens
 * are left unchanged. Escape sequences (backslash before the prefix)
 * are protected and restored after substitution.</p>
 *
 * <p>Thread Safety: Instances are effectively immutable after construction
 * and may be shared freely across threads.</p>
 *
 * @since 1.0
 * @see Mapping
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
@Getter
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

    // Characters with special meaning in Java regex
    private static final String REGEX_META = "\\.^§$*+?()[]{}|";
    // Regex capturing group for valid placeholder key characters
    private static final String KEY_REGEX = "([A-Za-z0-9_\\-]+)";
    // Internal sentinel used to protect escaped prefix sequences during apply()
    private static final String ESCAPED_PREFIX = "\u0001P";
    // Internal sentinel used to protect escaped suffix sequences during apply()
    private static final String ESCAPED_SUFFIX = "\u0001S";
    // Maximum allowed input length (1 MB) passed to apply()
    private static final int INPUT_LIMIT = 1_000_000;
    // Maximum number of substitutions performed in a single apply() call
    private static final int MAX_MATCHES = 10_000;

    private final @NonNull String prefix;
    private final @NonNull String suffix;
    private final @NonNull Pattern pattern;

    // null when prefix/suffix is empty (no escape processing needed)
    private final @Nullable String escapedPrefixLiteral;
    private final @Nullable String escapedSuffixLiteral;

    // ==== Helper Methods ====================================================

    // Escapes all regex metacharacters in {@code s} so the string can
    // be used as a literal inside a compiled {@link Pattern}.
    private static @NonNull String regexEscape(final @NonNull String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (REGEX_META.indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    // ==== Public API =======================================================

    /**
     * Constructs a new {@link MappingRule} with the given delimiter pair.
     *
     * <p>The prefix and suffix are escaped for use in a {@link Pattern}.
     * If the suffix is empty the rule matches open-ended tokens
     * (e.g. {@link #SHELL}: {@code $key}). Escape literals are computed
     * to enable backslash-escaping of the prefix during
     * {@link #apply(String, Set)}.</p>
     *
     * @param prefix the opening delimiter; must not be {@code null},
     *               may be empty only when suffix is also empty
     * @param suffix the closing delimiter; must not be {@code null},
     *               may be empty
     * @throws NullPointerException if {@code prefix} or {@code suffix}
     *                              is {@code null}
     * @throws IllegalArgumentException if both {@code prefix} and
     *                                  {@code suffix} are empty
     */
    public MappingRule(final @NonNull String prefix, final @NonNull String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;

        this.escapedPrefixLiteral = prefix.isEmpty() ? null : "\\" + prefix;
        this.escapedSuffixLiteral = suffix.isEmpty() ? null : "\\" + suffix;

        if (suffix.isEmpty()) {
            this.pattern = Pattern.compile(regexEscape(prefix) + KEY_REGEX, Pattern.UNICODE_CASE);
        } else {
            this.pattern = Pattern.compile(regexEscape(prefix) + KEY_REGEX + regexEscape(suffix));
        }
    }

    /**
     * Replaces all placeholder tokens in {@code input} with the values
     * from the supplied {@link Set} of {@link Mapping} instances.
     *
     * <p>The method first checks whether the prefix is present at all;
     * if not, {@code input} is returned as-is without any allocation.
     * When at least one match is possible, a lookup map is built from
     * the mappings and the compiled {@link #pattern} is applied.</p>
     *
     * <p>Escape sequences — a backslash immediately before the prefix —
     * are protected before substitution and restored afterward, allowing
     * callers to include literal prefix characters in the output.</p>
     *
     * <p>The method enforces two safety limits: inputs longer than
     * {@code 1 000 000} characters are rejected, and at most
     * {@code 10 000} substitutions are performed per call.</p>
     *
     * @param input    the source text to process; must not be {@code null}
     * @param mappings the set of key-value substitutions to apply;
     *                 must not be {@code null}
     * @return the substituted string; never {@code null}. Returns
     *         {@code input} unchanged when no tokens are found or
     *         {@code mappings} is empty.
     * @throws IllegalArgumentException if {@code input} exceeds
     *                                  {@code 1 000 000} characters
     * @throws NullPointerException     if {@code input} or {@code mappings}
     *                                  is {@code null}
     */
    public @NonNull String apply(final @NonNull String input,
                                 final @NonNull Set<Mapping> mappings) {
        if (input.length() > INPUT_LIMIT) throw new IllegalArgumentException("Input too large");
        if (mappings.isEmpty() || input.isEmpty()) return input;

        int firstPrefix = input.indexOf(prefix);
        if (firstPrefix < 0) return input;

        final Map<String, String> lookup = buildLookup(mappings);

        boolean hasEscape = !prefix.isEmpty() && input.indexOf('\\') >= 0
                && input.indexOf('\\' + prefix.charAt(0)) >= 0;

        String working = hasEscape ? protectEscapes(input) : input;

        Matcher matcher = pattern.matcher(working);

        StringBuilder sb = null;
        int lastEnd = 0;
        int matchCount = 0;

        while (matcher.find()) {
            if (++matchCount > MAX_MATCHES) break;

            String key = matcher.group(1);
            String replacement = lookup.get(key);
            if (replacement == null) continue;

            if (sb == null) {
                sb = new StringBuilder(working.length() + 32);
            }

            sb.append(working, lastEnd, matcher.start());
            sb.append(replacement);
            lastEnd = matcher.end();
        }

        if (sb == null) return input;

        sb.append(working, lastEnd, working.length());

        String result = sb.toString();
        return hasEscape ? restoreEscapes(result) : result;
    }

    // ==== Internal Implementation ===========================================

    // Builds a flat {@link Map} from key to string value for fast O(1)
    // lookup during the replacement loop in {@link #apply(String, Set)}.
    // For single-entry sets an immutable singleton map is returned to
    // avoid an unnecessary {@link HashMap} allocation.
    private @NonNull Map<String, String> buildLookup(final @NonNull Set<Mapping> mappings) {
        int size = mappings.size();
        if (size == 1) {
            Mapping m = mappings.iterator().next();
            return Map.of(m.key(), m.valueAsString());
        }
        Map<String, String> map = new HashMap<>((int) (size / 0.75f) + 1);
        for (Mapping m : mappings) map.put(m.key(), m.valueAsString());
        return map;
    }

    // Replaces escaped prefix and suffix sequences in {@code s} with
    // internal sentinel strings so they are not treated as token
    // delimiters during {@link #apply(String, Set)}.
    private @NonNull String protectEscapes(@NonNull String s) {
        if (escapedPrefixLiteral != null) s = s.replace(escapedPrefixLiteral, ESCAPED_PREFIX);
        if (escapedSuffixLiteral != null) s = s.replace(escapedSuffixLiteral, ESCAPED_SUFFIX);
        return s;
    }

    // Replaces internal sentinel strings back with the original prefix
    // and suffix characters after the substitution step in
    // {@link #apply(String, Set)}.
    private @NonNull String restoreEscapes(@NonNull String s) {
        if (escapedPrefixLiteral != null) s = s.replace(ESCAPED_PREFIX, prefix);
        if (escapedSuffixLiteral != null) s = s.replace(ESCAPED_SUFFIX, suffix);
        return s;
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
     * @param obj the object to compare; may be {@code null}
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