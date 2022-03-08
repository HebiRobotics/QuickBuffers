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

    private <T extends ProtoMessage> T readFromTruncated(T msg) throws IOException {
        byte[] data = msg.toByteArray();
        ProtoSource input = ProtoSource.newUnsafeInstance().wrap(data, 0, data.length - 1);
        msg.clear().mergeFrom(input);
        return msg;
    }

    // --------------------------------------------------------------------------------------

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testReadIntoTruncatedDestination() throws IOException {
        int numBytes = 10;
        ProtoSource.newInstance().wrap(new byte[numBytes])
                .readRawBytes(new byte[0], 0, 10);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testReadIntoTruncatedDestinationDirect() throws IOException {
        int numBytes = 10;
        ProtoSource.newUnsafeInstance().wrap(new byte[numBytes])
                .readRawBytes(new byte[0], 0, 10);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoNullDestination() throws IOException {
        int numBytes = 10;
        ProtoSource.newInstance().wrap(new byte[numBytes])
                .readRawBytes(null, 0, 10);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoNullDestinationDirect() throws IOException {
        int numBytes = 10;
        ProtoSource.newUnsafeInstance().wrap(new byte[numBytes])
                .readRawBytes(null, 0, 10);
    }

}
