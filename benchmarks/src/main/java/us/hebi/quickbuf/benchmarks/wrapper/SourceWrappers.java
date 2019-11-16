/*-
 * #%L
 * benchmarks
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

package us.hebi.quickbuf.benchmarks.wrapper;

import static us.hebi.quickbuf.benchmarks.UnsafeUtil.*;

/**
 * @author Florian Enner
 * @since 31 Jul 2019
 */
class SourceWrappers {

    static class ArrayWrapper {

        ArrayWrapper(byte[] bytes) {
            this.bytes = bytes;
            this.remaining = bytes.length;
        }

        boolean hasNext() {
            return remaining > 0;
        }

        boolean hasNext(int numBytes) {
            return remaining >= numBytes;
        }

        int next() {
            try {
                return bytes[position++] & 0xFF;
            } finally {
                remaining--;
            }
        }

        int nextInt() {
            try {
                return bytes[position++] << 24
                        | bytes[position++] << 16
                        | bytes[position++] << 8
                        | bytes[position++];
            } finally {
                remaining -= 4;
            }

        }

       /* int nextInt() {
            try {
                return UNSAFE.getInt(bytes, BYTE_ARRAY_OFFSET + position);
            } finally {
                position += 4;
                remaining -= 4;
            }
        }*/

        final byte[] bytes;
        int remaining = 0;
        int position = 0;

    }

    static class UnsafeWrapper {

        UnsafeWrapper(byte[] object, long offset, int length) {
            this.object = object;
            this.position = offset;
            this.limit = offset + length;
            this.remaining = length;
        }

        boolean hasNext() {
            return remaining > 0;
        }

        boolean hasNext(int numBytes) {
            return remaining >= numBytes;
        }

        int next() {
            try {
                return UNSAFE.getByte(object, position) & 0xFF;
            } finally {
                position++;
                remaining--;
            }
        }

        int nextInt() {
            try {
                return UNSAFE.getInt(object, position);
            } finally {
                position += 4;
                remaining -= 4;
            }
        }

        final byte[] object;
        final long limit;
        long position;
        int remaining;

    }

}
