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

    static Iterable<byte[]> getAllMessages() {
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
    static byte[] getCombinedMessage() throws IOException {
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

    static byte[] optionalPrimitives() {
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

    static byte[] repeatedPackablesNonPacked() {
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

    static byte[] repeatedPackablesPacked() {
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

    static byte[] optionalEnums() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalNestedEnum(NestedEnum.FOO)
                .setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR)
                .setOptionalImportEnum(ImportEnum.IMPORT_BAZ)
                .build();
        return msg.toByteArray();
    }

    static byte[] repeatedEnums() {
        return TestAllTypes.newBuilder()
                .addRepeatedNestedEnum(NestedEnum.FOO)
                .addRepeatedNestedEnum(NestedEnum.BAR)
                .addRepeatedNestedEnum(NestedEnum.BAZ)
                .addRepeatedNestedEnum(NestedEnum.BAZ)
                .build()
                .toByteArray();
    }

    static byte[] optionalString() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalString("optionalString\uD83D\uDCA9")
                .setOptionalCord("hello!")
                .build();
        return msg.toByteArray();
    }

    static byte[] optionalMessages() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalNestedMessage(TestAllTypes.NestedMessage.newBuilder().setBb(2).build())
                .setOptionalForeignMessage(ForeignMessage.newBuilder().setC(3).build())
                .build();
        return msg.toByteArray();
    }

    static byte[] repeatedMessages() {
        return TestAllTypes.newBuilder()
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(0))
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(1))
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(2))
                .build()
                .toByteArray();
    }

    static byte[] repeatedStrings() {
        return TestAllTypes.newBuilder()
                .addAllRepeatedString(Arrays.asList("hello", "world", "ascii", "utf8\uD83D\uDCA9"))
                .build()
                .toByteArray();
    }

    static byte[] optionalBytes() {
        byte[] randomBytes = new byte[256];
        new Random(0).nextBytes(randomBytes);
        return TestAllTypes.newBuilder()
                .setOptionalBytes(ByteString.copyFromUtf8("utf8\uD83D\uDCA9"))
                .setDefaultBytes(ByteString.copyFrom(randomBytes))
                .build()
                .toByteArray();
    }

    static byte[] repeatedBytes() {
        return TestAllTypes.newBuilder()
                .addRepeatedBytes(ByteString.copyFromUtf8("ascii"))
                .addRepeatedBytes(ByteString.copyFromUtf8("utf8\uD83D\uDCA9"))
                .build()
                .toByteArray();
    }

}


