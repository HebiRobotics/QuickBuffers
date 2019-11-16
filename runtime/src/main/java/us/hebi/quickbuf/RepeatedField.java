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
 * @since 10 Aug 2019
 */
abstract class RepeatedField<RepeatedType extends RepeatedField> {

    public final int length() {
        return length;
    }

    public final int remainingCapacity() {
        return capacity() - length;
    }

    /**
     * Makes sure that the internal storage capacity has at least
     * space for the requested number of entries. This method will
     * increase the internal capacity if needed.
     *
     * @param count number of entries to be added
     */
    @SuppressWarnings("unchecked")
    public final RepeatedType reserve(int count) {
        final int desiredSize = length + count;
        if (desiredSize > capacity()) {
            extendCapacityTo(desiredSize);
        }
        return (RepeatedType) this;
    }

    public void clear() {
        length = 0;
    }

    protected final void checkIndex(int index) {
        if (index < 0 || index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public abstract void copyFrom(RepeatedType other);

    public abstract int capacity();

    protected abstract void extendCapacityTo(int desiredSize);

    protected int length = 0;

}