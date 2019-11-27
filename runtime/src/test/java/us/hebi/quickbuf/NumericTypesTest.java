package us.hebi.quickbuf;

import org.junit.Before;
import org.junit.Test;
import protos.test.quickbuf.TestAllTypes;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 27 Nov 2019
 */
public class NumericTypesTest {

    @Before
    public void reset() {
        rnd.setSeed(0);
    }

    @Test
    public void testVarint32() throws IOException {
        testVarint32(0);
        testVarint32(1);
        testVarint32(-1);
        testVarint32(-128);
        testVarint32(Integer.MIN_VALUE);
        testVarint32(Integer.MAX_VALUE);
        testVarint32(1 << 7);
        testVarint32(1 << 14);
        testVarint32(1 << 21);
        testVarint32(1 << 28);
        testVarint32(~0 << 7);
        testVarint32(~0 << 14);
        testVarint32(~0 << 21);
        testVarint32(~0 << 28);
        for (int i = 1; i < n; i++) {
            testVarint32(rnd.nextInt() % (int) Math.pow(n, 3));
        }
    }

    @Test
    public void testVarint64() throws IOException {
        testVarint64(0);
        testVarint64(1);
        testVarint64(-1);
        testVarint64(-128);
        testVarint64(Long.MIN_VALUE);
        testVarint64(Long.MAX_VALUE);
        testVarint64(1L << 7);
        testVarint64(1L << 14);
        testVarint64(1L << 21);
        testVarint64(1L << 28);
        testVarint64(1L << 35);
        testVarint64(1L << 42);
        testVarint64(1L << 49);
        testVarint64(~0L << 7);
        testVarint64(~0L << 14);
        testVarint64(~0L << 21);
        testVarint64(~0L << 28);
        testVarint64(~0L << 35);
        testVarint64(~0L << 42);
        testVarint64(~0L << 49);
        for (int i = 1; i < n; i++) {
            testVarint64(rnd.nextLong() % (long) Math.pow(n, 6));
        }
    }

    @Test
    public void testFloat() throws IOException {
        testFloat(Float.POSITIVE_INFINITY);
        testFloat(Float.NEGATIVE_INFINITY);
        testFloat(Float.MAX_VALUE);
        testFloat(Float.MIN_VALUE);
        testFloat(Float.NaN);
        for (int i = 1; i < n; i++) {
            testFloat(rnd.nextFloat());
        }
    }

    @Test
    public void testDouble() throws IOException {
        testDouble(Double.POSITIVE_INFINITY);
        testDouble(Double.NEGATIVE_INFINITY);
        testDouble(Double.MAX_VALUE);
        testDouble(Double.MIN_VALUE);
        testDouble(Double.NaN);
        for (int i = 1; i < n; i++) {
            testDouble(rnd.nextDouble());
        }
    }

    private void testVarint32(int value) throws IOException {
        assertEquals(value, encodeAndDecode(msg.setOptionalInt32(value)).getOptionalInt32());
    }

    private void testVarint64(long value) throws IOException {
        assertEquals(value, encodeAndDecode(msg.setOptionalInt64(value)).getOptionalInt64());
    }

    private void testFloat(float value) throws IOException {
        assertEquals(Float.floatToIntBits(value), Float.floatToIntBits(encodeAndDecode(msg.setOptionalFloat(value)).getOptionalFloat()));
    }

    private void testDouble(double value) throws IOException {
        assertEquals(Double.doubleToLongBits(value),
                Double.doubleToLongBits(encodeAndDecode(msg.setOptionalDouble(value)).getOptionalDouble()));
    }

    private TestAllTypes encodeAndDecode(TestAllTypes msg) throws IOException {
        msg.writeTo(sink.wrap(bytes));
        msg.clear().mergeFrom(source.wrap(bytes, 0, sink.position()));
        return msg;
    }

    private Random rnd = new Random(0);
    private int n = 2000;
    private final TestAllTypes msg = TestAllTypes.newInstance();
    private final byte[] bytes = new byte[1024];
    private final ProtoSource source = ProtoSource.newInstance();
    private final ProtoSink sink = ProtoSink.newInstance();

}