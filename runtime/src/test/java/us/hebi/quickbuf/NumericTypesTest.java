/*-
 * #%L
 * quickbuf-runtime
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

    private void testVarint32(int value) throws IOException {
        assertEquals(value, encodeAndDecode(msg.setOptionalInt32(value)).getOptionalInt32());
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

    private void testVarint64(long value) throws IOException {
        assertEquals(value, encodeAndDecode(msg.setOptionalInt64(value)).getOptionalInt64());
    }

    @Test
    public void testInt32() throws IOException {
        for (int i = 0; i < 32; i++) {
            testInt32(1 << i);
            testInt32(~(1 << i));
            testInt32(~0 << i);
            testInt32(1 << i | Integer.MIN_VALUE);
            testInt32(~0 << i | Integer.MIN_VALUE);
            testInt32(-(1 << i));
        }
    }

    private void testInt32(int value) throws IOException {
        sink.reset().writeInt32NoTag(value);
        assertEquals(value, source.setInput(bytes).readInt32());
    }

    @Test
    public void testInt64() throws IOException {
        for (int i = 0; i < 64; i++) {
            testInt64(1L << i);
            testInt64(~(1L << i));
            testInt64(~0L << i);
            testInt64(1L << i | Long.MIN_VALUE);
            testInt64(~0L << i | Long.MIN_VALUE);
            testInt64(-(1L << i));
        }
    }

    private void testInt64(long value) throws IOException {
        sink.reset().writeInt64NoTag(value);
        assertEquals(value, source.setInput(bytes).readInt64());
    }

    @Test
    public void testExtendedInt32() throws IOException {
        for (int i = 0; i < 32; i++) {
            testExtendedInt32(1 << i);
            testExtendedInt32(~(1 << i));
            testExtendedInt32(~0 << i);
            testExtendedInt32(1 << i | Integer.MIN_VALUE);
            testExtendedInt32(~0 << i | Integer.MIN_VALUE);
            testExtendedInt32(-(1 << i));
        }
    }

    private void testExtendedInt32(int value) throws IOException {
        sink.reset().writeNegativeVarint32(value);
        assertEquals(value, source.setInput(bytes).readRawVarint32());
        assertEquals(value, (int) source.setInput(bytes).readRawVarint64SlowPath());
    }

    @Test
    public void testExtendedInt64() throws IOException {
        for (int i = 0; i < 64; i++) {
            testExtendedInt64(1L << i);
            testExtendedInt64(~(1L << i));
            testExtendedInt64(~0L << i);
            testExtendedInt64(1L << i | Long.MIN_VALUE);
            testExtendedInt64(~0L << i | Long.MIN_VALUE);
            testExtendedInt64(-(1L << i));
        }
    }

    private void testExtendedInt64(long value) throws IOException {
        sink.reset().writeNegativeVarint64(value);
        assertEquals(value, source.setInput(bytes).readRawVarint64SlowPath());
    }

    @Test
    public void testExtendedInt64Error() throws IOException {
        // there is an issue with the optimized implementation when
        // parsing non-canonical varints where positive values were
        // extended to 10 bytes. This should never occur in production.
        for (int i = 0; i < 63; i++) {
            testExtendedInt64Error(1L << i);
            testExtendedInt64Error(-(1L << i) ^ Long.MIN_VALUE);
        }

        // The issue does not apply when a varint has been extended to
        // fewer bits
        int value = 0x11;
        sink.reset().writeRawLittleEndian32(0x808080 | value);
        assertEquals(value, source.setInput(bytes).readRawVarint64SlowPath());
        assertEquals(value, source.setInput(bytes).readRawVarint64());
    }

    private void testExtendedInt64Error(long value) throws IOException {
        sink.reset().writeNegativeVarint64(value);
        assertEquals(value, source.setInput(bytes).readRawVarint64SlowPath());
        assertNotEquals(value, source.setInput(bytes).readRawVarint64());
    }

    @Test
    public void testUInt32() throws IOException {
        for (int i = 0; i < 32; i++) {
            testUInt32(1 << i);
            testUInt32(~(1 << i));
            testUInt32(~0 << i);
            testUInt32(1 << i | Integer.MIN_VALUE);
            testUInt32(~0 << i | Integer.MIN_VALUE);
            testUInt32(-(1 << i));
        }
    }

    private void testUInt32(int value) throws IOException {
        sink.reset().writeUInt32NoTag(value);
        assertEquals(value, source.setInput(bytes).readUInt32());
    }

    @Test
    public void testUInt64() throws IOException {
        for (int i = 0; i < 64; i++) {
            testUInt64(1L << i);
            testUInt64(~(1L << i));
            testUInt64(~0L << i);
            testUInt64(1L << i | Long.MIN_VALUE);
            testUInt64(~0L << i | Long.MIN_VALUE);
            testUInt64(-(1L << i));
        }
    }

    private void testUInt64(long value) throws IOException {
        sink.reset().writeUInt64NoTag(value);
        assertEquals(value, source.setInput(bytes).readUInt64());
    }

    @Test
    public void testFixed32() throws IOException {
        testFixed32(0);
        testFixed32(1);
        testFixed32(-1);
        testFixed32(-128);
        testFixed32(Integer.MIN_VALUE);
        testFixed32(Integer.MAX_VALUE);
        for (int i = 1; i < n; i++) {
            testFixed32(rnd.nextInt() % (int) Math.pow(n, 3));
        }
    }

    private void testFixed32(int value) throws IOException {
        assertEquals(value, encodeAndDecode(msg.setOptionalFixed32(value)).getOptionalFixed32());
    }

    @Test
    public void testFixed64() throws IOException {
        testFixed64(0);
        testFixed64(1);
        testFixed64(-1);
        testFixed64(-128);
        testFixed64(Long.MIN_VALUE);
        testFixed64(Long.MAX_VALUE);
        for (int i = 1; i < n; i++) {
            testFixed64(rnd.nextLong() % (long) Math.pow(n, 6));
        }
    }

    private void testFixed64(long value) throws IOException {
        assertEquals(value, encodeAndDecode(msg.setOptionalFixed64(value)).getOptionalFixed64());
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

    private void testFloat(float value) throws IOException {
        assertEquals(Float.floatToIntBits(value), Float.floatToIntBits(encodeAndDecode(msg.setOptionalFloat(value)).getOptionalFloat()));
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

    private void testDouble(double value) throws IOException {
        assertEquals(Double.doubleToLongBits(value),
                Double.doubleToLongBits(encodeAndDecode(msg.setOptionalDouble(value)).getOptionalDouble()));
    }

    private static void assertEqual(int expected, int actual){
        if(expected != actual) {
            fail("Values are not equal:\n" +
                    "Expected: " + Integer.toBinaryString(expected) + "\n" +
                    "Actual:   " + Integer.toBinaryString(actual));
        }
    }

    private static void assertEquals(long expected, long actual){
        if(expected != actual) {
            fail("Values are not equal:\n" +
                    "Expected: " + Long.toBinaryString(expected) + "\n" +
                    "Actual:   " + Long.toBinaryString(actual));
        }
    }

    private TestAllTypes encodeAndDecode(TestAllTypes msg) throws IOException {
        bytes.clear();
        msg.writeTo(sink.reset());
        msg.clear().mergeFrom(source.setInput(bytes));
        return msg;
    }

    private final Random rnd = new Random(0);
    private static final int n = 2000;
    private final TestAllTypes msg = TestAllTypes.newInstance();
    private final RepeatedByte bytes = RepeatedByte.newEmptyInstance();
    private final ProtoSource source = ProtoSource.newInstance(bytes);
    private final ProtoSink sink = ProtoSink.newInstance(bytes);

}
