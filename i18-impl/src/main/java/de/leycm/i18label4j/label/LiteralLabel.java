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
import de.leycm.i18label4j.mapping.MappingRule;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * A {@link Label} implementation that holds a fixed literal string.
 *
 * <p>{@link LiteralLabel} always returns its constructor-supplied string
 * regardless of the requested {@link Locale}. It is intended
 * for text that is not subject to localization, such as user-generated
 * content or technical identifiers that must be displayed verbatim.</p>
 *
 * <p>Placeholder {@link Mapping} objects can still be registered and are
 * applied by the default {@link MappingRule}
 * when {@link #mapped()} is called, allowing dynamic value substitution
 * even in non-localized labels.</p>
 *
 * <p>Thread Safety: This class is not thread-safe. The mutable
 * {@code mappings} set is not synchronized; instances must not be
 * modified and accessed from multiple threads concurrently.</p>
 *
 * @since 1.0
 * @see Label
 * @see LabelProvider#createLiteralLabel(String)
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
@SuppressWarnings("ClassCanBeRecord") // cause: mutable mappings
public class LiteralLabel implements Label {

    private final @NonNull LabelProvider provider;
    private final @NonNull Set<Mapping> mappings;
    private final @NonNull String literal;

    /**
     * Constructs a new {@link LiteralLabel} with an empty mapping set.
     *
     * @param provider the owning provider; must not be {@code null}
     * @param literal  the fixed text value; must not be {@code null}
     */
    public LiteralLabel(final @NonNull LabelProvider provider,
                        final @NonNull String literal) {
        this(provider, new HashSet<>(), literal);
    }

    /**
     * Constructs a new {@link LiteralLabel} with an existing mapping set.
     *
     * <p>This constructor is intended for internal use only, for example
     * when cloning an existing label with pre-populated mappings.</p>
     *
     * @param provider the owning provider; must not be {@code null}
     * @param mappings the initial set of mappings; must not be {@code null}
     * @param literal  the fixed text value; must not be {@code null}
     */
    @ApiStatus.Internal
    public LiteralLabel(final @NonNull LabelProvider provider,
                        final @NonNull Set<Mapping> mappings,
                        final @NonNull String literal) {
        this.provider = provider;
        this.mappings = mappings;
        this.literal = literal;
    }

    /**
     * Returns the owning {@link LabelProvider}.
     *
     * @return the provider; never {@code null}
     */
    @Override
    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    /**
     * Returns an unmodifiable view of all registered {@link Mapping} objects.
     *
     * @return the mappings; never {@code null}, may be empty
     */
    @Override
    public @NonNull Set<Mapping> getMappings() {
        return Collections.unmodifiableSet(mappings);
    }

    /**
     * Returns the fixed literal string held by this label.
     *
     * @return the literal; never {@code null}
     */
    public @NonNull String getLiteral() {
        return literal;
    }

    /**
     * {@inheritDoc}
     *
     * @param mapping the mapping to register; must not be {@code null}
     * @return this label for method chaining; never {@code null}
     * @throws IllegalArgumentException if a mapping with the same key
     *                                  already exists on this label
     */
    @Override
    public @NonNull Label mapTo(final @NonNull Mapping mapping) throws IllegalArgumentException {
        if (mappings.contains(mapping))
            throw new IllegalArgumentException(
                    "Mapping with key \"" + mapping.key() + "\" already exists for this label.");
        mappings.add(mapping);
        return this;
    }

    /**
     * Returns the literal string, ignoring the given locale entirely.
     *
     * @param locale the requested locale (ignored); must not be {@code null}
     * @return the literal string; never {@code null}
     */
    @Override
    public @NonNull String in(final @NonNull Locale locale) {
        return literal;
    }

    /**
     * Returns a string representation of this label.
     *
     * <p>Attempts to serialize the label to a {@link String} via the
     * registered serializer. If serialization is unavailable or throws,
     * falls back to a synthetic identity string of the form
     * {@code LiteralLabel@<hashCode>}.</p>
     *
     * @return a non-{@code null} string representation
     */
    @Override
    public @NonNull String toString() {
        try {
            return serialize(String.class);
        } catch (Throwable e) {
            return getClass().getSimpleName() +
                    "@" + Integer.toHexString(hashCode());
        }
    }

    /**
     * Determines equality based on provider identity and literal value.
     *
     * <p>Two {@link LiteralLabel} instances are equal when they share the
     * same {@link LabelProvider} and the same literal string.</p>
     *
     * @param obj the object to compare; may be {@code null}
     * @return {@code true} if {@code obj} is a {@link LiteralLabel} with
     *         an equal provider and literal
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LiteralLabel that = (LiteralLabel) obj;
        return provider.equals(that.getProvider()) &&
                literal.equals(that.getLiteral());
    }

    /**
     * Returns a hash code derived from the provider and literal,
     * consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(provider, literal);
    }
}