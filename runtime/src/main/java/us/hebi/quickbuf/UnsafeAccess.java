/*-
 * #%L
 * MAT File Library
 * %%
 * Copyright (C) 2018 HEBI Robotics
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

    public static boolean isAvailable() {
        return UNSAFE != null;
    }

    public static boolean isCopyMemoryAvailable(){
        // copy memory (obj, long, obj, long, long) is @since 1.7
        return isAvailable() && javaVersion >= 7;
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


    // Move this into a separate utility class if anything else should ever
    // depend on the version
    static {
        String specVersion = System.getProperty("java.specification.version", "1.6");
        String[] parts = specVersion.split("\\.");

        // 1.4, 1.5, 1.6, 1.8, 9, 10, 11, 18.3, etc.
        if ("1".equals(parts[0])) {
            javaVersion = Integer.parseInt(parts[1]);
        } else {
            javaVersion = Integer.parseInt(parts[0]);
        }
    }

    private static final int javaVersion;

}
