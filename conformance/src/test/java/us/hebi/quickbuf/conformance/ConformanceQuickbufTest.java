/*-
 * #%L
 * conformance
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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
package us.hebi.quickbuf.conformance;

import com.google.quickbuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2;
import org.junit.Test;
import us.hebi.quickbuf.JsonSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 02 Feb 2023
 */
public class ConformanceQuickbufTest {

    @Test
    public void testValidDataRepeatedBoolUnpacked() throws IOException {
        assertEqualsProto2("Recommended.Proto2.ProtobufInput.ValidDataRepeated.BOOL.UnpackedInput.DefaultOutput.ProtobufOutput", "\330\002\000\330\002\001\330\002\001\330\002\001\330\002\000\330\002\001\330\002\000",
                -40, 2, 0, -40, 2, 1, -40, 2, 1, -40, 2, 1, -40, 2, 0, -40, 2, 1, -40, 2, 0);
    }

    @Test
    public void testValidDataRepeatedEnumPacked() throws IOException {
        assertEqualsProto2("Recommended.Proto2.ProtobufInput.ValidDataRepeated.ENUM.PackedInput.PackedOutput.ProtobufOutput",
                "\302\005 \000\001\002\377\377\377\377\377\377\377\377\377\001\377\377\377\377\377\377\377\377\177\201\200\200\200\200\200\200\200\200\001",
                -62, 5, 24, 0, 1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, 1);
    }

    @Test
    public void testRepeatedScalarSelectsLast() throws IOException {
        assertEqualsProto2("Required.Proto2.ProtobufInput.RepeatedScalarSelectsLast.BOOL.ProtobufOutput",
                "h\000h\001h\377\377\377\377\377\377\377\377\377\001h\316\302\361\005h\200\200\200\200 h\377\377\377\377\377\377\377\377\177h\200\200\200\200\200\200\200\200\200\001",
                104, 1);
    }

    @Test
    public void testRepeatedUint32Packed() throws IOException {
        assertEqualsProto2("Required.Proto2.ProtobufInput.ValidDataRepeated.UINT32.PackedInput.ProtobufOutput",
                "\212\0027\000\271`\271\340\200\000\271\340\200\200\200\200\200\200\000\377\377\377\377\017\200\200\200\200 \201\200\200\200 \377\377\377\377\037\377\377\377\377\377\377\377\377\177\201\200\200\200\200\200\200\200\200\001",
                -120, 2, 0, -120, 2, -71, 96, -120, 2, -71, 96, -120, 2, -71, 96, -120, 2, -1, -1, -1, -1, 15, -120, 2, 0, -120, 2, 1, -120, 2, -1, -1, -1, -1, 15, -120, 2, -1, -1, -1, -1, 15, -120, 2, 1);
    }

    @Test
    public void testValidDataOneOfBinaryMessage() throws IOException {
        assertEqualsProto2("Recommended.Proto2.ProtobufInput.ValidDataOneofBinary.MESSAGE.Merge.ProtobufOutput",
                "\202\007\t\022\007\010\001\020\001\310\005\001\202\007\007\022\005\020\001\310\005\001",
                -126, 7, 12, 18, 10, 8, 1, 16, 1, -56, 5, 1, -56, 5, 1);
    }

    private void assertEqualsProto2(String name, String request, int... bytes) throws IOException {
        TestAllTypesProto2 msg = TestAllTypesProto2.parseFrom(request.getBytes(StandardCharsets.ISO_8859_1));
        byte[] expected = new byte[bytes.length];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) bytes[i];
        }
        assertArrayEquals(name, expected, msg.toByteArray());
    }

    // for debugging to generate the expected output. Needs -Pwith-protobuf
/*    private void assertEqualsProto2(String name, String requestString) throws IOException {
        byte[] request = requestString.getBytes(ISO_8859_1);
        com.google.protobuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2 protobuf = com.google.protobuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2.parseFrom(request);
        TestAllTypesProto2 quickbuf = TestAllTypesProto2.parseFrom(request);
        byte[] expected = protobuf.toByteArray();
        byte[] actual = quickbuf.toByteArray();
        System.out.println(Arrays.toString(expected));
        if (!Arrays.equals(expected, actual)) {
            assertEquals(JsonFormat.printer().omittingInsignificantWhitespace().print(protobuf), JsonSink.newInstance().writeMessage(quickbuf).toString());
            fail(name + " has same JSON output, but wrong binary representation");
        }
    }*/

    @Test
    public void testJsonInputFieldNameExtensionValidator() throws IOException {
        // Tests whether the parser fails on extended fields. This is not officially supported
        // by most clients, but it is part of the conformance tests.
        // see https://github.com/apple/swift-protobuf/issues/993
        String name = "Recommended.Proto2.JsonInput.FieldNameExtension.Validator";
        String input = "{\n        \"[protobuf_test_messages.proto2.extension_int32]\": 1\n      }";
        TestAllTypesProto2 msg = TestAllTypesProto2.parseFrom(JsonSource
                .newInstance(input)
                .setIgnoreUnknownFields(false));
        assertEquals(1, msg.getExtensionInt32());
        assertEquals("" +
                "{\n" +
                "  \"[protobuf_test_messages.proto2.extension_int32]\": 1\n" +
                "}", msg.toString());
    }

}
