/*-
 * #%L
 * MAT File Library
 * %%
 * Copyright (C) 2018 HEBI Robotics
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

package us.hebi.robobuf;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * @author Florian Enner
 * @since 31 Aug 2018
 */
class UnsafeAccess {

    public static void requireUnsafe() {
        // throws an exception if not available
        if (!isAvailable())
            throw new AssertionError("Unsafe is not available on this platform");
    }

    public static boolean isAvailable() {
        return UNSAFE != null;
    }

    static {
        Unsafe unsafe = null;
        long byteArrayOffset = 0;
        long booleanArrayOffset = 0;
        long floatArrayOffset = 0;
        long intArrayOffset = 0;
        long doubleArrayOffset = 0;
        long longArrayOffset = 0;
        try {
            final PrivilegedExceptionAction<Unsafe> action =
                    new PrivilegedExceptionAction<Unsafe>() {
                        @Override
                        public Unsafe run() throws Exception {
                            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                            f.setAccessible(true);

                            return (Unsafe) f.get(null);
                        }
                    };

            unsafe = AccessController.doPrivileged(action);
            byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
            booleanArrayOffset = unsafe.arrayBaseOffset(boolean[].class);
            floatArrayOffset = unsafe.arrayBaseOffset(float[].class);
            intArrayOffset = unsafe.arrayBaseOffset(int[].class);
            doubleArrayOffset = unsafe.arrayBaseOffset(double[].class);
            longArrayOffset = unsafe.arrayBaseOffset(long[].class);

        } catch (final Exception ex) {
            // Not available
        }

        UNSAFE = unsafe;
        BYTE_ARRAY_OFFSET = byteArrayOffset;
        BOOLEAN_ARRAY_OFFSET = booleanArrayOffset;
        FLOAT_ARRAY_OFFSET = floatArrayOffset;
        INT_ARRAY_OFFSET = intArrayOffset;
        DOUBLE_ARRAY_OFFSET = doubleArrayOffset;
        LONG_ARRAY_OFFSET = longArrayOffset;
    }

    final static ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    final static Unsafe UNSAFE;
    final static long BYTE_ARRAY_OFFSET;
    final static long BOOLEAN_ARRAY_OFFSET;
    final static long FLOAT_ARRAY_OFFSET;
    final static long INT_ARRAY_OFFSET;
    final static long DOUBLE_ARRAY_OFFSET;
    final static long LONG_ARRAY_OFFSET;

}
