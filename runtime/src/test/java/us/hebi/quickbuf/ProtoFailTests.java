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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static protos.test.quickbuf.UnittestRequired.*;

/**
 * @author Florian Enner
 * @since 15 Aug 2019
 */
public class ProtoFailTests {

    @Test(expected = UninitializedMessageException.class)
    public void testMissingRequiredField() {
        try {
            SimpleMessage.newInstance().toByteArray();
        } catch (UninitializedMessageException ex) {
            assertEquals(1, ex.getMissingFields().size());
            throw ex;
        }
    }

    @Test(expected = UninitializedMessageException.class)
    public void testMissingRequiredFieldAll() {
        try {
            TestAllTypesRequired.newInstance()
                    .setRequiredNestedMessage(SimpleMessage.newInstance())
                    .toByteArray();
        } catch (UninitializedMessageException ex) {
            List<String> missing = ex.getMissingFields();
            // uncomment to generate copy paste values
            /*for (String s : missing) {
                System.out.println("\"" + s + "\",");
            }*/
            assertEquals(17, missing.size());
            assertEquals(missing, Arrays.asList(
                    "required_double",
                    "required_fixed64",
                    "required_sfixed64",
                    "required_int64",
                    "required_uint64",
                    "required_sint64",
                    "required_float",
                    "required_fixed32",
                    "required_sfixed32",
                    "required_int32",
                    "required_uint32",
                    "required_sint32",
                    "required_nested_enum",
                    "required_bool",
                    "required_nested_message.required_field",
                    "required_bytes",
                    "required_string"
            ));
            throw ex;
        }
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testCheckInitializedRequired() throws InvalidProtocolBufferException {
        TestAllTypesRequired.newInstance().checkInitialized();
    }

    @Test
    public void testCheckInitializedOptional() throws InvalidProtocolBufferException {
        TestAllTypes.newInstance().checkInitialized();
    }

    @Test(expected = UninitializedMessageException.class)
    public void testParseFromUninitialized() throws Throwable {
        try {
            SimpleMessage.parseFrom(new byte[0]);
        } catch (InvalidProtocolBufferException e) {
            throw e.getCause();
        }
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
        ProtoSink output = ProtoSink.newDirectSink().setOutput(buffer);
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
        ProtoSource input = ProtoSource.newDirectSource().setInput(data, 0, data.length - 1);
        msg.clear().mergeFrom(input);
        return msg;
    }

    // --------------------------------------------------------------------------------------
    static final int n = 10;

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoTruncatedDestinationArray() throws IOException {
        ProtoSource.newArraySource().setInput(new byte[n]).readRawBytes(new byte[0], 0, n);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoTruncatedDestinationDirect() throws IOException {
        ProtoSource.newDirectSource().setInput(new byte[n]).readRawBytes(new byte[0], 0, n);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoTruncatedDestinationStream() throws IOException {
        ProtoSource.newInstance(new ByteArrayInputStream(new byte[n])).readRawBytes(new byte[0], 0, n);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoTruncatedDestinationBuffer() throws IOException {
        ProtoSource.newInstance(ByteBuffer.wrap(new byte[n])).readRawBytes(new byte[0], 0, n);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoNullDestination() throws IOException {
        ProtoSource.newArraySource().setInput(new byte[n]).readRawBytes(null, 0, n);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoNullDestinationDirect() throws IOException {
        ProtoSource.newDirectSource().setInput(new byte[n]).readRawBytes(null, 0, n);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoNullDestinationStream() throws IOException {
        ProtoSource.newInstance(new ByteArrayInputStream(new byte[n])).readRawBytes(null, 0, n);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoNullDestinationBuffer() throws IOException {
        ProtoSource.newInstance(ByteBuffer.wrap(new byte[n])).readRawBytes(null, 0, n);
    }

}
