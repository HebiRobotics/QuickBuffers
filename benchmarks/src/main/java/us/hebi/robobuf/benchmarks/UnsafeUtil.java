package us.hebi.robobuf.benchmarks;

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
