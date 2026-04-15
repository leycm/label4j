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

public class LocaleLabel implements Label, Comparable<LocaleLabel> {
    private final @NonNull Set<Placeholder> placeholders;
    private final @NonNull String key;
    private final @NonNull LabelProvider provider;

    @ApiStatus.Internal
    public LocaleLabel(
            final @NonNull String key,
            final @NonNull LabelProvider provider
    ) {
        this.placeholders = ConcurrentHashMap.newKeySet();
        this.key = key;
        this.provider = provider;
    }

    public @NonNull String key() {
        return key;
    }

    @Override
    public @NonNull LabelProvider getProvider() {
        return provider;
    }

    @Override
    public @UnmodifiableView @NonNull Set<Placeholder> getPlaceholders() {
        return Collections.unmodifiableSet(placeholders);
    }

    @Override
    public @NonNull Label replace(@NonNull Set<@NonNull Placeholder> placeholders) throws DuplicatePlaceholderException {
        for (final Placeholder placeholder : placeholders) {
            if (this.placeholders.contains(placeholder)) {
                throw new DuplicatePlaceholderException(placeholder.key());
            }

            this.placeholders.add(placeholder);
        }

        return this;
    }

    @Override
    public @NonNull Localization localize(final @NonNull Locale locale) {
        return getProvider().localize(locale, key);
    }

    @Override
    public @NonNull String toString() {
        return Label.class.getSimpleName() + "[locale]{" +
                "key='" + key + '\'' +
                ", provider=" + provider +
                '}';
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        LocaleLabel that = (LocaleLabel) object;
        return Objects.equals(key, that.key) && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, provider);
    }

    @Override
    public int compareTo(final @NotNull LocaleLabel that) {
        return this.key.compareTo(that.key);
    }
}
