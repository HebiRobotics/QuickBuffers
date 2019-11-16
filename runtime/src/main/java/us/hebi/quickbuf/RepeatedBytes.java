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
public class RepeatedBytes extends RepeatedObject<RepeatedBytes, RepeatedByte, byte[]> {

    public static RepeatedBytes newEmptyInstance(){
        return new RepeatedBytes();
    }

    private RepeatedBytes(){
    }

    @Override
    protected void copyDataFrom0(RepeatedBytes other) {
        for (int i = 0; i < other.length; i++) {
            array[i].copyFrom(other.array[i]);
        }
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected void setIndex0(int index, byte[] value) {
        array[index].copyFrom(value);
    }

    @Override
    protected boolean isEqual(RepeatedByte a, Object b) {
        return a.equals(b);
    }

    @Override
    protected RepeatedByte createEmpty() {
        return new RepeatedByte();
    }

    @Override
    protected RepeatedByte[] allocateArray0(int desiredSize) {
        return new RepeatedByte[desiredSize];
    }

}
