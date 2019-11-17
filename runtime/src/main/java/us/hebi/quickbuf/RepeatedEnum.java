/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.quickbuf;

import us.hebi.quickbuf.ProtoEnum.EnumConverter;

/**
 * Values are internally stored as an int array. This class
 * contains helper methods to convert to and from the enum
 * representations.
 *
 * @author Florian Enner
 * @since 17 Nov 2019
 */
public class RepeatedEnum<E extends ProtoEnum> extends RepeatedEnumBase {

    @SuppressWarnings("unchecked")
    public static <E extends ProtoEnum> RepeatedEnum<E> newEmptyInstance(EnumConverter<E> converter) {
        return new RepeatedEnum(converter);
    }

    private RepeatedEnum(EnumConverter<E> converter) {
        if (converter == null)
            throw new NullPointerException();
        this.converter = converter;
    }

    public E get(int index) {
        return converter.forNumber(getNumber(index));
    }

    public RepeatedEnumBase set(int index, E value) {
        checkIndex(index);
        array[index] = value.getNumber();
        return this;
    }

    public RepeatedEnumBase add(final E value) {
        reserve(1);
        array[length++] = value.getNumber();
        return this;
    }

    public RepeatedEnumBase addAll(final E[] values) {
        return addAll(values, 0, values.length);
    }

    public RepeatedEnumBase addAll(final E[] buffer, final int offset, final int length) {
        final int pos = this.length;
        setLength(pos + length);
        for (int i = 0; i < length; i++) {
            array[pos + i] = buffer[i].getNumber();
        }
        return this;
    }

    public RepeatedEnumBase copyFrom(final E[] buffer) {
        return copyFrom(buffer, 0, buffer.length);
    }

    public RepeatedEnumBase copyFrom(final E[] buffer, final int offset, final int length) {
        this.length = 0;
        addAll(buffer);
        return this;
    }

    final EnumConverter<E> converter;

}
