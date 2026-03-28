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
package de.leycm.i18label4j.source;

import de.leycm.i18label4j.LabelProvider;

import lombok.NonNull;

import java.util.NoSuchElementException;
import java.io.IOException;
import java.util.*;

/**
 * Abstraction over a storage backend that provides raw translation maps.
 *
 * <p>A {@link LocalizationSource} knows which {@link Locale}s are available
 * and can load the complete key-to-value translation map for any of them.
 * Concrete implementations may read from the classpath, the file system,
 * a remote URL, an in-memory map, or any other source.</p>
 *
 * <p>The interface deliberately declares {@link #getLocalization(Locale)}
 * with a checked {@link Exception} so that implementations are free to
 * propagate IO, parsing, or network exceptions to callers — typically the
 * {@link LabelProvider} which decides how to handle
 * them (e.g. by caching an empty map and throwing an
 * {@link IllegalArgumentException}).</p>
 *
 * <p>Thread Safety: Implementations are not required to be thread-safe.
 * The {@code CommonLabelProvider} serializes access
 * to the source through its own internal cache, so source implementations
 * are typically only called once per locale.</p>
 *
 * @since 1.0
 * @see LabelProvider
 * @author Lennard <a href="mailto:leycm@proton.me">leycm@proton.me</a>
 */
public interface LocalizationSource extends Iterable<Locale> {

    /**
     * Returns the set of locales that this source can provide translations for.
     *
     * <p>The returned set reflects what the source has discovered (e.g.
     * directory entries or registered keys) at the time of the call. It
     * may be a snapshot and is not necessarily live.</p>
     *
     * @return a non-{@code null}, possibly empty set of available locales;
     *         never {@code null}
     */
    @NonNull Set<Locale> getLocalizations();

    /**
     * Loads and returns the complete translation map for the given locale.
     *
     * <p>The returned map contains all key-value pairs available for the
     * locale. Keys are plain strings (e.g. {@code "menu.start"}), values
     * are the raw translation strings. The map may be mutable or
     * immutable depending on the implementation; callers must not rely
     * on either guarantee.</p>
     *
     * <p>Note: the exception behavior (which exception types are thrown
     * and under which conditions) is defined by the implementation.
     * Common examples include {@link NoSuchElementException}
     * when the locale is unknown, or {@link IOException} when
     * the underlying resource cannot be read.</p>
     *
     * @param locale the locale to load translations for; never {@code null}
     * @return a map from translation key to translated value; never {@code null}
     * @throws Exception if the translations cannot be loaded for any reason;
     *                   the exact exception type is implementation-defined
     */
    @NonNull Map<String, String> getLocalization(@NonNull Locale locale) throws Exception;

    /**
     * Returns {@code true} if this source contains translations for the
     * given locale.
     *
     * <p>The default implementation delegates to {@link #getLocalizations()}
     * and checks for membership. Implementations may override this with a
     * more efficient check that does not require materializing the full set.</p>
     *
     * @param locale the locale to test; never {@code null}
     * @return {@code true} if the locale is available; {@code false} otherwise
     */
    default boolean containsLocalization(@NonNull Locale locale) {
        // note: logic can and should be replaced with a more efficient implementation
        return getLocalizations().contains(locale);
    }

    /**
     * Returns an {@link Iterator} over all available locales.
     *
     * <p>The default implementation delegates to the iterator of the set
     * returned by {@link #getLocalizations()}.</p>
     *
     * @return an iterator; never {@code null}
     */
    @Override
    default @NonNull Iterator<Locale> iterator() {
        return getLocalizations().iterator();
    }
}