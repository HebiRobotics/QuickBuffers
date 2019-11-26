/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

import org.junit.Test;
import protos.test.quickbuf.RepeatedPackables;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.UnittestRequired;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 15 Aug 2019
 */
public class ProtoFailTests {

    @Test(expected = IllegalStateException.class)
    public void testMissingRequiredField() {
        UnittestRequired.SimpleMessage.newInstance().toByteArray();
    }

    // --------------------------------------------------------------------------------------

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceVarint32() throws IOException {
        // id is at the very end, so don't set it for other types
        writeToTruncated(TestAllTypes.newInstance().setId(Integer.MIN_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceVarint64() throws IOException {
        writeToTruncated(TestAllTypes.newInstance().setDefaultInt64(Long.MIN_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceFixed32() throws IOException {
        writeToTruncated(TestAllTypes.newInstance().setDefaultFixed32(Integer.MAX_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceFixed64() throws IOException {
        writeToTruncated(TestAllTypes.newInstance().setDefaultFixed64(Long.MAX_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceString() throws IOException {
        writeToTruncated(TestAllTypes.newInstance().setOptionalString("this should fail"));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceBytes() throws IOException {
        writeToTruncated(TestAllTypes.newInstance().addAllDefaultBytes(new byte[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedBoolean() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllBools(new boolean[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedDouble() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllDoubles(new double[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedFloat() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllFloats(new float[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedVarint32() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllInt32S(new int[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedFixed32() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllFixed32S(new int[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedVarint64() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllInt64S(new long[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedFixed64() throws IOException {
        writeToTruncated(RepeatedPackables.Packed.newInstance().addAllFixed64S(new long[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalNestedMessage().setBb(1);
        writeToTruncated(msg);
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceGroup() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalGroup().setA(2);
        writeToTruncated(msg);
    }

    private void writeToTruncated(ProtoMessage msg) throws IOException {
        byte[] buffer = new byte[msg.getSerializedSize() - 1];
        // use unsafe to make sure that we aren't relying on normal array checks
        ProtoSink output = ProtoSink.newUnsafeInstance().wrap(buffer);
        msg.writeTo(output);
    }

    // --------------------------------------------------------------------------------------

    @Test
    public void testMalformedVarint32() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        RepeatedByte bytes = msg.getUnknownBytes();
        Arrays.fill(bytes.setLength(12).array(), (byte) -5); // bad value

        byte[] array = msg.getUnknownBytes().reserve(20).array();
        array[0] = 1 << 3; // optional int32 in msg

        // 12 byte varint
        try {
            bytes.setLength(13).array[12] = 0;
            TestAllTypes.parseFrom(msg.toByteArray());
            fail();
        } catch (InvalidProtocolBufferException e) {
            assertTrue(e.getMessage().contains("malformed"));
        }

        // 11 byte varint
        try {
            bytes.setLength(12).array[11] = 0;
            TestAllTypes.parseFrom(msg.toByteArray());
            fail();
        } catch (InvalidProtocolBufferException e) {
            assertTrue(e.getMessage().contains("malformed"));
        }

        // 10 byte -> should work
        bytes.setLength(11).array[10] = 0;
        TestAllTypes.parseFrom(msg.toByteArray());

    }

    @Test
    public void testMalformedVarint64() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        RepeatedByte bytes = msg.getUnknownBytes();
        Arrays.fill(bytes.setLength(12).array(), (byte) -5); // bad value

        byte[] array = msg.getUnknownBytes().reserve(20).array();
        array[0] = 2 << 3; // optional int64 in msg

        // 12 byte varint
        try {
            bytes.setLength(13).array[12] = 0;
            TestAllTypes.parseFrom(msg.toByteArray());
            fail();
        } catch (InvalidProtocolBufferException e) {
            assertTrue(e.getMessage().contains("malformed"));
        }

        // 11 byte varint
        try {
            bytes.setLength(12).array[11] = 0;
            TestAllTypes.parseFrom(msg.toByteArray());
            fail();
        } catch (InvalidProtocolBufferException e) {
            assertTrue(e.getMessage().contains("malformed"));
        }

        // 10 byte -> should work
        bytes.setLength(11).array[10] = 0;
        TestAllTypes.parseFrom(msg.toByteArray());

    }

    @Test
    public void testVarint32() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        testVarint32(msg, 0);
        testVarint32(msg, 1);
        testVarint32(msg, -1);
        testVarint32(msg, -128);
        testVarint32(msg, Integer.MIN_VALUE);
        testVarint32(msg, Integer.MAX_VALUE);
        testVarint32(msg, 1 << 7);
        testVarint32(msg, 1 << 14);
        testVarint32(msg, 1 << 21);
        testVarint32(msg, 1 << 28);
        testVarint32(msg, ~0 << 7);
        testVarint32(msg, ~0 << 14);
        testVarint32(msg, ~0 << 21);
        testVarint32(msg, ~0 << 28);
    }

    private void testVarint32(TestAllTypes msg, int value) throws IOException {
        msg.setOptionalInt32(value);
        byte[] bytes = msg.toByteArray();
        msg.clear().mergeFrom(ProtoSource.newInstance(bytes));
        assertEquals(value, msg.getOptionalInt32());
    }
    
    @Test
    public void testVarint64() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        testVarint64(msg, 0);
        testVarint64(msg, 1);
        testVarint64(msg, -1);
        testVarint64(msg, -128);
        testVarint64(msg, Long.MIN_VALUE);
        testVarint64(msg, Long.MAX_VALUE);
        testVarint64(msg, 1L << 7);
        testVarint64(msg, 1L << 14);
        testVarint64(msg, 1L << 21);
        testVarint64(msg, 1L << 28);
        testVarint64(msg, 1L << 35);
        testVarint64(msg, 1L << 42);
        testVarint64(msg, 1L << 49);
        testVarint64(msg, ~0L << 7);
        testVarint64(msg, ~0L << 14);
        testVarint64(msg, ~0L << 21);
        testVarint64(msg, ~0L << 28);
        testVarint64(msg, ~0L << 35);
        testVarint64(msg, ~0L << 42);
        testVarint64(msg, ~0L << 49);
    }

    private void testVarint64(TestAllTypes msg, long value) throws IOException {
        msg.setOptionalInt64(value);
        byte[] bytes = msg.toByteArray();
        msg.clear().mergeFrom(ProtoSource.newInstance(bytes));
        assertEquals(value, msg.getOptionalInt64());
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedVarint32() throws IOException {
        readFromTruncated(TestAllTypes.newInstance().setId(Integer.MIN_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedVarint64() throws IOException {
        readFromTruncated(TestAllTypes.newInstance().setDefaultInt64(Long.MIN_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedFixed32() throws IOException {
        readFromTruncated(TestAllTypes.newInstance().setDefaultFixed32(Integer.MAX_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedFixed64() throws IOException {
        readFromTruncated(TestAllTypes.newInstance().setDefaultFixed64(Long.MAX_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedString() throws IOException {
        readFromTruncated(TestAllTypes.newInstance().setOptionalString("this should fail"));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedBytes() throws IOException {
        readFromTruncated(TestAllTypes.newInstance().addAllDefaultBytes(new byte[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedBoolean() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllBools(new boolean[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedDouble() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllDoubles(new double[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedFloat() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllFloats(new float[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedVarint32() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllInt32S(new int[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedFixed32() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllFixed32S(new int[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedVarint64() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllInt64S(new long[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedFixed64() throws IOException {
        readFromTruncated(RepeatedPackables.Packed.newInstance().addAllFixed64S(new long[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalNestedMessage().setBb(1);
        readFromTruncated(msg);
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedGroup() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalGroup().setA(2);
        readFromTruncated(msg);
    }

    private void readFromTruncated(ProtoMessage msg) throws IOException {
        byte[] data = msg.toByteArray();
        ProtoSource input = ProtoSource.newUnsafeInstance().wrap(data, 0, data.length - 1);
        msg.clear().mergeFrom(input);
    }

    // --------------------------------------------------------------------------------------

}
