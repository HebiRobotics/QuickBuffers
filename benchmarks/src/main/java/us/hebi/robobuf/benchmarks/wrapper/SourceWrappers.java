package us.hebi.robobuf.benchmarks.wrapper;

import static us.hebi.robobuf.benchmarks.UnsafeUtil.*;

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
