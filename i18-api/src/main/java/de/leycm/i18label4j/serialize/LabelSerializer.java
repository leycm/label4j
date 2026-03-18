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
package de.leycm.i18label4j.serialize;

import de.leycm.i18label4j.Label;
import de.leycm.i18label4j.exception.*;

import lombok.NonNull;

public interface LabelSerializer<T> {

    @NonNull T serialize(@NonNull Label label) throws SerializationException;

    @NonNull Label deserialize(@NonNull T serialized) throws DeserializationException;

    @NonNull T format(@NonNull String input) throws FormatException;

}
