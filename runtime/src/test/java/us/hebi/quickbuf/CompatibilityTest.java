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

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import org.junit.Ignore;
import org.junit.Test;
import protos.test.protobuf.ForeignEnum;
import protos.test.protobuf.ForeignMessage;
import protos.test.protobuf.RepeatedPackables;
import protos.test.protobuf.TestAllTypes;
import protos.test.protobuf.TestAllTypes.NestedEnum;
import protos.test.protobuf.external.ImportEnum;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * "ground-truth" data generated by protobuf-Java
 *
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public class CompatibilityTest {

    /**
     * Make sure Java bindings are still equal after doing a round-trip
     */
    @Test
    public void testCompatibilityWithProtobufJava() throws IOException {
        byte[] serializedMsg = getCombinedMessage();
        TestAllTypes.Builder expected = TestAllTypes.newBuilder();
        protos.test.quickbuf.TestAllTypes msg = protos.test.quickbuf.TestAllTypes.newInstance();

        // multiple merges to check expanding repeated behavior
        for (int i = 0; i < 3; i++) {
            expected.mergeFrom(serializedMsg);
            msg.mergeFrom(ProtoSource.newInstance(serializedMsg));
        }

        assertEquals(expected.build(), TestAllTypes.parseFrom(msg.toByteArray()));
        assertEquals(msg, protos.test.quickbuf.TestAllTypes.parseFrom(msg.toByteArray()));

    }

    @Test
    public void testCompatibilityWithProtobufJava_Unsafe() throws IOException {
        byte[] serializedMsg = getCombinedMessage();
        TestAllTypes.Builder expected = TestAllTypes.newBuilder();
        protos.test.quickbuf.TestAllTypes msg = protos.test.quickbuf.TestAllTypes.newInstance();

        // multiple merges to check expanding repeated behavior
        for (int i = 0; i < 3; i++) {
            expected.mergeFrom(serializedMsg);
            msg.mergeFrom(ProtoSource.newDirectSource().setInput(serializedMsg));
        }

        assertEquals(expected.build(), TestAllTypes.parseFrom(msg.toByteArray()));
        assertEquals(msg, protos.test.quickbuf.TestAllTypes.parseFrom(msg.toByteArray()));

    }

    public static Iterable<byte[]> getAllMessages() {
        return Arrays.asList(
                optionalBytes(),
                optionalPrimitives(),
                optionalMessages(),
                optionalBytes(),
                optionalEnums(),
                optionalString(),
                repeatedMessages(),
                repeatedBytes(),
                repeatedStrings(),
                repeatedBytes()
        );
    }

    // Update ProtoTests::testMergeFromMessage
    public static byte[] getCombinedMessage() throws IOException {
        TestAllTypes.Builder msg = TestAllTypes.newBuilder();
        for (byte[] bytes : getAllMessages()) {
            msg.mergeFrom(bytes);
        }
        return msg
                .addAllRepeatedPackedInt32(Arrays.asList(-1, 0, 1, 2, 3, 4, 5))
                .addAllRepeatedInt32(Arrays.asList(-2, -1, 0, 1, 2, 3, 4, 5))
                .build()
                .toByteArray();
    }

    public static byte[] optionalPrimitives() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalBool(true)
                .setOptionalDouble(100.0d)
                .setOptionalFloat(101.0f)
                .setOptionalFixed32(102)
                .setOptionalFixed64(103)
                .setOptionalSfixed32(104)
                .setOptionalSfixed64(105)
                .setOptionalSint32(106)
                .setOptionalSint64(107)
                .setOptionalInt32(108)
                .setOptionalInt64(109)
                .setOptionalUint32(110)
                .setOptionalUint64(111)
                .build();
        return msg.toByteArray();
    }

    public static byte[] repeatedPackablesNonPacked() {
        RepeatedPackables.NonPacked msg = RepeatedPackables.NonPacked.newBuilder()
                .addAllBools(Arrays.asList(true, false, true, true))
                .addAllDoubles(Arrays.asList(Double.POSITIVE_INFINITY, -2d, 3d, 4d))
                .addAllFloats(Arrays.asList(10f, 20f, -30f, Float.NaN))
                .addAllFixed32S(Arrays.asList(2, -2, 4, 67423))
                .addAllFixed64S(Arrays.asList(3231313L, 6L, -7L, 8L))
                .addAllSfixed32S(Arrays.asList(2, -3, 4, 5))
                .addAllSfixed64S(Arrays.asList(5L, -6L, 7L, -8L))
                .addAllSint32S(Arrays.asList(2, -3, 4, 5))
                .addAllSint64S(Arrays.asList(5L, 6L, -7L, 8L))
                .addAllInt32S(Arrays.asList(2, 3, -4, 5))
                .addAllInt64S(Arrays.asList(5L, -6L, 7L, 8L))
                .addAllUint32S(Arrays.asList(2, 300, 4, 5))
                .addAllUint64S(Arrays.asList(5L, 6L, 23L << 40, 8L))
                .build();
        return msg.toByteArray();
    }

    public static byte[] repeatedPackablesPacked() {
        RepeatedPackables.Packed msg = RepeatedPackables.Packed.newBuilder()
                .addAllBools(Arrays.asList(true, false, true, true))
                .addAllDoubles(Arrays.asList(Double.POSITIVE_INFINITY, -2d, 3d, 4d))
                .addAllFloats(Arrays.asList(10f, 20f, -30f, Float.NaN))
                .addAllFixed32S(Arrays.asList(2, -2, 4, 67423))
                .addAllFixed64S(Arrays.asList(3231313L, 6L, -7L, 8L))
                .addAllSfixed32S(Arrays.asList(2, -3, 4, 5))
                .addAllSfixed64S(Arrays.asList(5L, -6L, 7L, -8L))
                .addAllSint32S(Arrays.asList(2, -3, 4, 5))
                .addAllSint64S(Arrays.asList(5L, 6L, -7L, 8L))
                .addAllInt32S(Arrays.asList(2, 3, -4, 5))
                .addAllInt64S(Arrays.asList(5L, -6L, 7L, 8L))
                .addAllUint32S(Arrays.asList(2, 300, 4, 5))
                .addAllUint64S(Arrays.asList(5L, 6L, 23L << 40, 8L))
                .build();
        return msg.toByteArray();
    }

    public static byte[] optionalEnums() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalNestedEnum(NestedEnum.FOO)
                .setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR)
                .setOptionalImportEnum(ImportEnum.IMPORT_BAZ)
                .build();
        return msg.toByteArray();
    }

    public static byte[] repeatedEnums() {
        return TestAllTypes.newBuilder()
                .addRepeatedNestedEnum(NestedEnum.FOO)
                .addRepeatedNestedEnum(NestedEnum.BAR)
                .addRepeatedNestedEnum(NestedEnum.BAZ)
                .addRepeatedNestedEnum(NestedEnum.BAZ)
                .build()
                .toByteArray();
    }

    public static byte[] optionalString() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalString("optionalString\uD83D\uDCA9")
                .setOptionalCord("hello!")
                .build();
        return msg.toByteArray();
    }

    public static byte[] optionalMessages() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalNestedMessage(TestAllTypes.NestedMessage.newBuilder().setBb(2).build())
                .setOptionalForeignMessage(ForeignMessage.newBuilder().setC(3).build())
                .setOptionalGroup(TestAllTypes.OptionalGroup.newBuilder().setA(4))
                .build();
        return msg.toByteArray();
    }

    public static byte[] repeatedMessages() {
        return TestAllTypes.newBuilder()
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(0))
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(1))
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(2))
                .addRepeatedGroup(TestAllTypes.RepeatedGroup.newBuilder().setA(3))
                .addRepeatedGroup(TestAllTypes.RepeatedGroup.newBuilder().setA(4))
                .build()
                .toByteArray();
    }

    public static byte[] repeatedStrings() {
        return TestAllTypes.newBuilder()
                .addAllRepeatedString(Arrays.asList("hello", "world", "ascii", "utf8\uD83D\uDCA9"))
                .build()
                .toByteArray();
    }

    public static byte[] optionalBytes() {
        byte[] randomBytes = new byte[256];
        new Random(0).nextBytes(randomBytes);
        return TestAllTypes.newBuilder()
                .setOptionalBytes(ByteString.copyFromUtf8("utf8\uD83D\uDCA9"))
                .setDefaultBytes(ByteString.copyFrom(randomBytes))
                .build()
                .toByteArray();
    }

    public static byte[] repeatedBytes() {
        return TestAllTypes.newBuilder()
                .addRepeatedBytes(ByteString.copyFromUtf8("ascii"))
                .addRepeatedBytes(ByteString.copyFromUtf8("utf8\uD83D\uDCA9"))
                .build()
                .toByteArray();
    }

    @Test
    public void testProtobufJavaJsonParser() {
        TestAllTypes msg;

        msg = parseJson(JSON_EMPTY);
        assertEquals(0, msg.getSerializedSize());

        msg = parseJson(JSON_OBJECT_TYPES_NULL);
        assertEquals(3, msg.getSerializedSize());

        msg = parseJson(JSON_ALL_TYPES_NULL);
        assertEquals(0, msg.getSerializedSize());

        msg = parseJson(JSON_SPECIAL_NUMBERS);
        assertEquals(111, msg.getSerializedSize());

        testError(JSON_REPEATED_BYTES_NULL_VALUE, "Repeated field elements cannot be null in field: quickbuf_unittest.TestAllTypes.repeated_bytes");
        testError(JSON_REPEATED_MSG_NULL_VALUE, "Repeated field elements cannot be null in field: quickbuf_unittest.TestAllTypes.repeated_foreign_message");
        testError(JSON_NULL, "Expect message object but got: null");
        testError(JSON_LIST_EMPTY, "Expect message object but got: []");
        testError(JSON_BAD_BOOLEAN, "Invalid bool value: \"fals\"");
        testError(JSON_UNKNOWN_FIELD, "Cannot find field: unknownField in message quickbuf_unittest.TestAllTypes");
        testError(JSON_UNKNOWN_FIELD_NULL, "Cannot find field: unknownField in message quickbuf_unittest.TestAllTypes");

    }

    @Ignore
    @Test
    public void testProtobufJavaJsonParserManualInput() {
        // This fails on Github CI on a ubuntu-22.04 runner due to the error below. It looks like
        // it's trying to reflectively call the wrong method, but somehow it works on other systems?
        //
        // Caused by: java.lang.IllegalArgumentException: EnumValueDescriptor is not for this type.
        //	at protos.test.protobuf.ForeignEnum.valueOf(ForeignEnum.java:96)
        //
        assertEquals(591, parseJson(JSON_MANUAL_INPUT).getSerializedSize());
    }

    private static TestAllTypes parseJson(String input) {
        try {
            TestAllTypes.Builder builder = TestAllTypes.newBuilder();
            JsonFormat.parser().merge(input, builder);
            return builder.build();
        } catch (Throwable e) {
            throw new IllegalArgumentException(input, e);
        }
    }

    private void testError(String input, String error) {
        try {
            JsonFormat.parser().merge(input, TestAllTypes.newBuilder());
            fail("expected error: " + error);
        } catch (IOException ioe) {
            assertEquals(error, ioe.getMessage());
        }
    }

    public static String JSON_MANUAL_INPUT = "{\n" +
            "  \"optionalDouble\": 100,\n" +
            "  \"optionalFixed64\": 103,\n" +
            "  \"optionalSfixed64\": 105,\n" +
            "  \"optionalInt64\": 109,\n" +
            "  \"optionalUint64\": 111,\n" +
            "  \"optionalSint64\": 107,\n" +
            "  \"optionalFloat\": 101,\n" +
            "  \"optionalFixed32\": 102,\n" +
            "  \"optionalSfixed32\": 104,\n" +
            "  \"optionalInt32\": 108,\n" +
            "  \"optionalUint32\": 110,\n" +
            "  \"optionalSint32\": 106,\n" +
            "  \"optionalNestedEnum\": \"FOO\",\n" +
            "  \"optionalForeignEnum\": \"FOREIGN_BAR\",\n" +
            "  \"optionalImportEnum\": \"IMPORT_BAZ\",\n" +
            "  \"optionalBool\": true,\n" +
            "  \"optionalNestedMessage\": {\n" +
            "    \"bb\": 2\n" +
            "  },\n" +
            "  \"optionalForeignMessage\": {\n" +
            "    \"c\": 3\n" +
            "  },\n" +
            "  \"optionalImportMessage\": {},\n" +
            "  \"optionalBytes\": \"dXRmOPCfkqk=\",\n" +
            "  \"defaultBytes\": \"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\n" +
            "  \"optionalString\": \"optionalString\\\\escape\\t\\b\\n\\funi\uD83D\uDCA9\",\n" +
            "  \"optionalCord\": \"hello!\",\n" +
            "  \"repeatedDouble\": [\n" +
            "    \"NaN\",\n" +
            "    \"-Infinity\",\n" +
            "    0,\n" +
            "    -28.3\n" +
            "  ],\n" +
            "  \"repeatedFloat\": [],\n" +
            "  \"repeatedInt32\": [\n" +
            "    -2,\n" +
            "    -1,\n" +
            "    0,\n" +
            "    1,\n" +
            "    2,\n" +
            "    3,\n" +
            "    4,\n" +
            "    5\n" +
            "  ],\n" +
            "  \"repeatedPackedInt32\": [\n" +
            "    -1,\n" +
            "    0,\n" +
            "    1,\n" +
            "    2,\n" +
            "    3,\n" +
            "    4,\n" +
            "    5\n" +
            "  ],\n" +
            "  \"repeatedForeignMessage\": [\n" +
            "    {\n" +
            "      \"c\": 0\n" +
            "    },\n" +
            "    {\n" +
            "      \"c\": 1\n" +
            "    },\n" +
            "    {\n" +
            "      \"c\": 2\n" +
            "    },\n" +
            "    {},\n" +
            "    {}\n" +
            "  ],\n" +
            "  \"repeatedBytes\": [\n" +
            "    \"YXNjaWk=\",\n" +
            "    \"dXRmOPCfkqk=\",\n" +
            "    \"YXNjaWk=\",\n" +
            "    \"dXRmOPCfkqk=\",\n" +
            "    \"\"\n" +
            "  ],\n" +
            "  \"repeatedString\": [\n" +
            "    \"hello\",\n" +
            "    \"world\",\n" +
            "    \"ascii\",\n" +
            "    \"utf8\uD83D\uDCA9\"\n" +
            "  ]\n" +
            "}";

    public static final String JSON_OBJECT_TYPES_NULL = "{\"optionalNestedMessage\":null,\"repeatedString\":null,\"optionalForeignMessage\":{},\"repeatedBytes\":null}";

    public static final String JSON_ALL_TYPES_NULL = "{\n" +
            "  \"optionalDouble\": null,\n" +
            "  \"optionalFixed64\": null,\n" +
            "  \"optionalSfixed64\": null,\n" +
            "  \"optionalInt64\": null,\n" +
            "  \"optionalUint64\": null,\n" +
            "  \"optionalSint64\": null,\n" +
            "  \"optionalFloat\": null,\n" +
            "  \"optionalFixed32\": null,\n" +
            "  \"optionalSfixed32\": null,\n" +
            "  \"optionalInt32\": null,\n" +
            "  \"optionalUint32\": null,\n" +
            "  \"optionalSint32\": null,\n" +
            "  \"optionalNestedEnum\": null,\n" +
            "  \"optionalForeignEnum\": null,\n" +
            "  \"optionalImportEnum\": null,\n" +
            "  \"optionalBool\": null,\n" +
            "  \"optionalNestedMessage\": null,\n" +
            "  \"optionalForeignMessage\": null,\n" +
            "  \"optionalImportMessage\": null,\n" +
            "  \"optionalBytes\": null,\n" +
            "  \"defaultBytes\": null,\n" +
            "  \"optionalString\": null,\n" +
            "  \"optionalCord\": null,\n" +
            "  \"repeatedDouble\": null,\n" +
            "  \"repeatedFloat\": null,\n" +
            "  \"repeatedInt32\": null,\n" +
            "  \"repeatedPackedInt32\": null,\n" +
            "  \"repeatedForeignMessage\": null,\n" +
            "  \"repeatedBytes\": null,\n" +
            "  \"repeatedString\": null\n" +
            "}";

    public static String JSON_NULL = "null";

    public static String JSON_EMPTY = "{}";

    public static String JSON_LIST_EMPTY = "[]";

    public static String JSON_BAD_BOOLEAN = "{\"optionalBool\": fals}";
    public static String JSON_UNKNOWN_FIELD = "{\"unknownField\": false}";
    public static String JSON_UNKNOWN_FIELD_NULL = "{\"unknownField\": null}";

    public static String JSON_REPEATED_BYTES_NULL_VALUE = "{\"repeatedBytes\": [null,null]}";

    public static String JSON_REPEATED_MSG_NULL_VALUE = "{\"repeatedForeignMessage\": [null,null]}";

    public static final String JSON_SPECIAL_NUMBERS = "{\n" +
            "  \"repeated_double\": [\n" +
            "    \"NaN\",\n" +
            "    \"-Infinity\",\n" +
            "    0,\n" +
            "    -28.3,\n" +
            "    3E6,\n" +
            "    -0,\n" +
            "    17E-3,\n" +
            "    Infinity\n" +
            "  ],\n" +
            "  \"repeated_int32\": [\n" +
            "    \"0\",\n" +
            "    \"2147483647\",\n" +
            "    -2147483648,\n" +
            "    0,\n" +
            "    1,\n" +
            "    2\n" +
            "  ]\n" +
            "}";

    public static final String JSON_ROOT_LIST = "[" +
            JSON_EMPTY + ",\n" +
            JSON_SPECIAL_NUMBERS + ",\n" +
            JSON_MANUAL_INPUT +
            "]";


}


