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
public final class RepeatedString extends RepeatedObject<RepeatedString, StringBuilder, CharSequence> {

    public static RepeatedString newEmptyInstance(){
        return new RepeatedString();
    }

    RepeatedString(){
    }

    @Override
    protected void setIndex0(int index, CharSequence value) {
        array[index].setLength(0);
        array[index].append(value);
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].setLength(0);
    }

    @Override
    protected boolean isEqual(StringBuilder a, Object b) {
        return (b instanceof CharSequence) && ProtoUtil.isEqual(a, (CharSequence) b);
    }

    @Override
    protected StringBuilder[] allocateArray0(int desiredSize) {
        return new StringBuilder[desiredSize];
    }

    @Override
    protected StringBuilder createEmpty() {
        return new StringBuilder(0);
    }

    @Override
    protected void copyDataFrom0(RepeatedString other) {
        for (int i = 0; i < length; i++) {
            setIndex0(i, other.array[i]);
        }
    }

}
