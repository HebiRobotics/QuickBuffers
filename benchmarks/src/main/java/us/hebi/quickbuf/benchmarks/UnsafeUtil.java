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

package us.hebi.quickbuf.benchmarks;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * @author Florian Enner
 * @since 31 Jul 2019
 */
public class UnsafeUtil {

    public static final Unsafe UNSAFE;
    public static final long BYTE_ARRAY_OFFSET;

    static {
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
            UNSAFE = AccessController.doPrivileged(action);
            BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

        } catch (final Exception ex) {
            throw new AssertionError("Unsafe not available");
        }

    }

    public static long getDirectAddress(ByteBuffer buffer) {
        return ((DirectBuffer) buffer).address();
    }

}
