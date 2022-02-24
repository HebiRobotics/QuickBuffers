/*-
 * #%L
 * benchmarks
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
