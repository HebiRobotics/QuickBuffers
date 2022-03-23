/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.quickbuf;

import java.util.Iterator;

/**
 * @author Florian Enner
 * @since 10 Aug 2019
 */
abstract class RepeatedField<RepeatedType extends RepeatedField, GenericType> implements Iterable<GenericType> {

    public final int length() {
        return length;
    }

    public final int remainingCapacity() {
        return capacity() - length;
    }

    protected abstract GenericType getValueAt(int index);

    @Override
    public GenericIterator iterator() {
        return new GenericIterator(length);
    }

    class GenericIterator implements Iterator<GenericType> {

        GenericIterator(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public boolean hasNext() {
            return position < maxLength;
        }

        @Override
        public GenericType next() {
            return getValueAt(position++);
        }

        private int position = 0;
        private final int maxLength;

    }

    /**
     * Makes sure that the internal storage capacity has space for
     * at least the requested number of entries. This method will
     * increase the internal capacity if needed.
     *
     * @param count number of entries to be added
     */
    @SuppressWarnings("unchecked")
    public final RepeatedType reserve(int count) {
        final int desiredSize = length + count;
        if (desiredSize - capacity() > 0) { // overflow-conscious
            extendCapacityTo(desiredSize);
        }
        return (RepeatedType) this;
    }

    /**
     * Sets the output length to zero and performs any
     * necessary cleanup of the content. Does not release
     * the internal buffer.
     */
    @SuppressWarnings("unchecked")
    public RepeatedType clear() {
        length = 0;
        return (RepeatedType) this;
    }

    protected final void checkIndex(int index) {
        if (index < 0 || index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public abstract void addAll(RepeatedType values);

    public abstract void copyFrom(RepeatedType other);

    public abstract int capacity();

    protected abstract void extendCapacityTo(int desiredSize);

    protected int length = 0;

    /**
     * Repeated fields have no immutable state and should not
     * be used in hashing structures. This method returns
     * a constant value.
     *
     * @return 0
     */
    @Override
    public final int hashCode() {
        return 0;
    }

}
