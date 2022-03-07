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

package us.hebi.quickbuf;

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

    static boolean isAvailable() {
        return UNSAFE != null && ENABLE_UNSAFE;
    }

    static boolean isCopyMemoryAvailable() {
        return ENABLE_UNSAFE_COPY;
    }

    static boolean allowUnalignedAccess() {
        return ENABLE_UNSAFE_UNALIGNED;
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

    static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    static final Unsafe UNSAFE;
    static final long BYTE_ARRAY_OFFSET;
    static final long BOOLEAN_ARRAY_OFFSET;
    static final long FLOAT_ARRAY_OFFSET;
    static final long INT_ARRAY_OFFSET;
    static final long DOUBLE_ARRAY_OFFSET;
    static final long LONG_ARRAY_OFFSET;

    // Move this into a separate utility class if anything else should ever
    // depend on the version
    static {
        String specVersion = System.getProperty("java.specification.version", "1.6");
        String[] parts = specVersion.split("\\.");

        if ("0.9".equals(specVersion)) {
            // Android
            MAJOR_JAVA_VERSION = 6;
        } else if ("1".equals(parts[0])) {
            // 1.4, 1.5, 1.6, 1.8
            MAJOR_JAVA_VERSION = Integer.parseInt(parts[1]);
        } else {
            // 9, 10, 11, 18.3, etc.
            MAJOR_JAVA_VERSION = Integer.parseInt(parts[0]);
        }
    }

    static final int MAJOR_JAVA_VERSION;
    static final boolean IS_ARM = Boolean.getBoolean("jvm.isarm")
            || System.getProperty("os.arch", "N/A").startsWith("arm")
            || System.getProperty("os.arch", "N/A").startsWith("aarch");
    static final boolean ENABLE_UNSAFE = !Boolean.getBoolean("quickbuf.disable_unsafe_access");
    static final boolean ENABLE_UNSAFE_UNALIGNED = ENABLE_UNSAFE && !IS_ARM && !Boolean.getBoolean("quickbuf.disable_unaligned_access");

    // copy memory (obj, long, obj, long, long) is @since 1.7
    static final boolean ENABLE_UNSAFE_COPY = ENABLE_UNSAFE && MAJOR_JAVA_VERSION >= 7;

}
