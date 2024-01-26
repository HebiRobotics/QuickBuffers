/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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

/**
 * This class dispatches calls to built-in Java methods
 * when available, and runs a fallback version when not.
 * It allows us to make use of newer features without
 * breaking backwards compatibility with oder runtimes.
 *
 * @author Florian Enner
 * @since 14 Jan 2023
 */
public class JdkMath {

    /**
     * Returns as a {@code long} the most significant 64 bits of the 128-bit
     * product of two 64-bit factors.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     * @since 9
     */
    public static long multiplyHigh(long x, long y) {
        if (HAS_MULTIPLY_HIGH) {
            return JdkMethods.multiplyHigh(x, y);
        }
        if (x < 0 || y < 0) {
            // Use technique from section 8-2 of Henry S. Warren, Jr.,
            // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
            long x1 = x >> 32;
            long x2 = x & 0xFFFFFFFFL;
            long y1 = y >> 32;
            long y2 = y & 0xFFFFFFFFL;
            long z2 = x2 * y2;
            long t = x1 * y2 + (z2 >>> 32);
            long z1 = t & 0xFFFFFFFFL;
            long z0 = t >> 32;
            z1 += x2 * y1;
            return x1 * y1 + z0 + (z1 >> 32);
        } else {
            // Use Karatsuba technique with two base 2^32 digits.
            long x1 = x >>> 32;
            long y1 = y >>> 32;
            long x2 = x & 0xFFFFFFFFL;
            long y2 = y & 0xFFFFFFFFL;
            long A = x1 * y1;
            long B = x2 * y2;
            long C = (x1 + x2) * (y1 + y2);
            long K = C - A - B;
            return (((B >>> 32) + K) >>> 32) + A;
        }
    }

    private static boolean hasMultiplyHigh0() {
        try {
            JdkMethods.multiplyHigh(0, 0);
            return true;
        } catch (NoSuchMethodError oldRuntime) {
            return false;
        }
    }

    static final boolean HAS_MULTIPLY_HIGH = hasMultiplyHigh0();

}
