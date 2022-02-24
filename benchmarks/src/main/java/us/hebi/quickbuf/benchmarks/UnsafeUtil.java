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
