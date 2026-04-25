package de.leycm.label4j.label;

import de.leycm.label4j.Label;
import de.leycm.label4j.LabelProvider;
import de.leycm.label4j.exception.DuplicatePlaceholderException;
import de.leycm.label4j.localization.Localization;
import de.leycm.label4j.placeholder.Placeholder;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LiteralLabel implements Label, Comparable<LiteralLabel> {
    private final @NonNull Set<Placeholder> placeholders;
    private final @NonNull LabelProvider provider;
    private final @NonNull String literal;

    @ApiStatus.Internal
    public LiteralLabel(
            final @NonNull LabelProvider provider,
            final @NonNull String literal
    ) {
        this.placeholders = ConcurrentHashMap.newKeySet();
        this.provider = provider;
        this.literal = literal;
    }

    // ==== Field Methods =====================================================

    @Override
    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    @Override
    public @UnmodifiableView @NonNull Set<Placeholder> getPlaceholders() {
        return Collections.unmodifiableSet(placeholders);
    }

    // ==== Replacement Methods ===============================================

    @Override
    public @NonNull Label replace(
            final @NonNull Set<@NonNull Placeholder> placeholders
    ) throws DuplicatePlaceholderException {
        for (final Placeholder placeholder : placeholders) {
            if (this.placeholders.contains(placeholder)) {
                throw new DuplicatePlaceholderException(placeholder.key());
            }

            this.placeholders.add(placeholder);
        }

        return this;
    }

    // ==== Resolution Methods ================================================

    @Override
    public @NonNull Localization localize(@NonNull Locale locale) {
        return Localization.literal(locale, literal);
    }

    // ==== Object Methods ====================================================

    @Override
    public @NonNull String toString() {
        return Label.class.getSimpleName() + "[literal]{" +
                "literal='" + literal + '\'' +
                ", provider=" + provider +
                '}';
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        LiteralLabel that = (LiteralLabel) object;
        return Objects.equals(literal, that.literal) && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(literal, provider);
    }

    @Override
    public int compareTo(final @NotNull LiteralLabel that) {
        return this.literal.compareTo(that.literal);
    }
}
