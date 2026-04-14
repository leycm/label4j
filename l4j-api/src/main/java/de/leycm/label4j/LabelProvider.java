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
package de.leycm.label4j;

import de.leycm.init4j.instance.Instanceable;
import de.leycm.label4j.exception.*;
import de.leycm.label4j.placeholder.PlaceholderRule;

import lombok.NonNull;

public interface LabelProvider extends Instanceable {

    static @NonNull LabelProvider getInstance() {
        return Instanceable.getInstance(LabelProvider.class);
    }

    @NonNull PlaceholderRule getDefaultPlaceholderRule();

    // ==== Serialization ====================================================

    <T> @NonNull T serialize(@NonNull Label label,
                             @NonNull Class<T> type)
            throws SerializationException;

    <T> @NonNull Label deserialize(@NonNull T serialized)
            throws DeserializationException;

    <T> @NonNull T format(@NonNull String input,
                          @NonNull Class<T> type)
            throws FormatException;
}
