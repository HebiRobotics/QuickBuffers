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

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class RepeatedString extends RepeatedObject<RepeatedString, Utf8String, CharSequence, String> {

    public static RepeatedString newEmptyInstance() {
        return new RepeatedString();
    }

    RepeatedString() {
    }

    public String get(final int index, final Utf8Decoder decoder) {
        checkIndex(index);
        return array[index].getString(decoder);
    }

    @Override
    protected void setIndex0(int index, CharSequence value) {
        array[index].copyFrom(value);
    }

    @Override
    protected String getIndex0(int index) {
        return array[index].getString();
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected void copyFrom0(Utf8String store, Utf8String other) {
        store.copyFrom(other);
    }

    @Override
    protected Utf8String[] allocateArray0(int desiredSize) {
        return new Utf8String[desiredSize];
    }

    @Override
    protected Utf8String createEmpty() {
        return Utf8String.newEmptyInstance();
    }

}
