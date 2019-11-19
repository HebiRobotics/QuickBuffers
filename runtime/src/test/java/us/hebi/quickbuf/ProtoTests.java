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
import protos.test.quickbuf.*;
import protos.test.quickbuf.TestAllTypes.NestedEnum;
import protos.test.quickbuf.UnittestFieldOrder.MessageWithMultibyteNumbers;
import protos.test.quickbuf.UnittestRequired.TestAllTypesRequired;
import protos.test.quickbuf.external.ImportEnum;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.quickbuf.ProtoUtil.Charsets.*;

/**
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public class ProtoTests {

    @Test
    public void testDefaults() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        for (int i = 0; i < 2; i++) {
            assertTrue(msg.getDefaultBool());
            assertEquals(41, msg.getDefaultInt32());
            assertEquals(42, msg.getDefaultInt64());
            assertEquals(43, msg.getDefaultUint32());
            assertEquals(44, msg.getDefaultUint64());
            assertEquals(-45, msg.getDefaultSint32());
            assertEquals(46, msg.getDefaultSint64());
            assertEquals(47, msg.getDefaultFixed32());
            assertEquals(48, msg.getDefaultFixed64());
            assertEquals(49, msg.getDefaultSfixed32());
            assertEquals(-50, msg.getDefaultSfixed64());
            assertEquals(51.5f, msg.getDefaultFloat(), 0);
            assertEquals(52.0e3, msg.getDefaultDouble(), 0.0);
            assertEquals("hello", msg.getDefaultString().toString());
            assertEquals("world", new String(msg.getDefaultBytes().toArray(), UTF_8));
            assertEquals("dünya", msg.getDefaultStringNonascii().toString());
            assertEquals("dünyab", new String(msg.getDefaultBytesNonascii().toArray(), UTF_8));
            assertEquals(NestedEnum.BAR, msg.getDefaultNestedEnum());
            assertEquals(ForeignEnum.FOREIGN_BAR, msg.getDefaultForeignEnum());
            assertEquals(ImportEnum.IMPORT_BAR, msg.getDefaultImportEnum());
            assertEquals(Float.POSITIVE_INFINITY, msg.getDefaultFloatInf(), 0);
            assertEquals(Float.NEGATIVE_INFINITY, msg.getDefaultFloatNegInf(), 0);
            assertEquals(Float.NaN, msg.getDefaultFloatNan(), 0);
            assertEquals(Double.POSITIVE_INFINITY, msg.getDefaultDoubleInf(), 0);
            assertEquals(Double.NEGATIVE_INFINITY, msg.getDefaultDoubleNegInf(), 0);
            assertEquals(Double.NaN, msg.getDefaultDoubleNan(), 0);

            byte[] result = msg.toByteArray();
            int msgSerializedSize = msg.getSerializedSize();
            assertEquals(0, msgSerializedSize);
            assertEquals(result.length, msgSerializedSize);
            msg.clear();
        }
    }

    @Test
    public void testOptionalPrimitives() throws IOException {
        TestAllTypes emptyMsg = TestAllTypes.newInstance();
        assertFalse(emptyMsg.hasOptionalBool());
        assertFalse(emptyMsg.hasOptionalDouble());
        assertFalse(emptyMsg.hasOptionalFloat());
        assertFalse(emptyMsg.hasOptionalFixed32());
        assertFalse(emptyMsg.hasOptionalFixed64());
        assertFalse(emptyMsg.hasOptionalSfixed32());
        assertFalse(emptyMsg.hasOptionalSfixed64());
        assertFalse(emptyMsg.hasOptionalSint32());
        assertFalse(emptyMsg.hasOptionalSint64());
        assertFalse(emptyMsg.hasOptionalInt32());
        assertFalse(emptyMsg.hasOptionalInt64());
        assertFalse(emptyMsg.hasOptionalUint32());
        assertFalse(emptyMsg.hasOptionalUint64());

        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.optionalPrimitives());
        assertNotEquals(msg, emptyMsg);

        assertTrue(msg.hasOptionalBool());
        assertTrue(msg.hasOptionalDouble());
        assertTrue(msg.hasOptionalFloat());
        assertTrue(msg.hasOptionalFixed32());
        assertTrue(msg.hasOptionalFixed64());
        assertTrue(msg.hasOptionalSfixed32());
        assertTrue(msg.hasOptionalSfixed64());
        assertTrue(msg.hasOptionalSint32());
        assertTrue(msg.hasOptionalSint64());
        assertTrue(msg.hasOptionalInt32());
        assertTrue(msg.hasOptionalInt64());
        assertTrue(msg.hasOptionalUint32());
        assertTrue(msg.hasOptionalUint64());

        assertTrue(msg.getOptionalBool());
        assertEquals(100.0d, msg.getOptionalDouble(), 0);
        assertEquals(101.0f, msg.getOptionalFloat(), 0);
        assertEquals(102, msg.getOptionalFixed32());
        assertEquals(103, msg.getOptionalFixed64());
        assertEquals(104, msg.getOptionalSfixed32());
        assertEquals(105, msg.getOptionalSfixed64());
        assertEquals(106, msg.getOptionalSint32());
        assertEquals(107, msg.getOptionalSint64());
        assertEquals(108, msg.getOptionalInt32());
        assertEquals(109, msg.getOptionalInt64());
        assertEquals(110, msg.getOptionalUint32());
        assertEquals(111, msg.getOptionalUint64());

        TestAllTypes manualMsg = TestAllTypes.newInstance()
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
                .setOptionalUint64(111);
        assertEquals(msg, manualMsg);

        // Test round-trip
        TestAllTypes msg3 = TestAllTypes.parseFrom(manualMsg.toByteArray());
        assertEquals(msg, msg3);

        // Test quick clear
        TestAllTypes msg4 = msg3.clone();
        assertEquals(msg3.clear(), msg4.clearQuick());
        assertArrayEquals(msg3.toByteArray(), msg4.toByteArray());

    }

    @Test
    public void testRepeatedPrimitives() throws IOException {
        RepeatedPackables.Packed emptyMsg = RepeatedPackables.Packed.newInstance();
        assertFalse(emptyMsg.hasBools());
        assertFalse(emptyMsg.hasDoubles());
        assertFalse(emptyMsg.hasFloats());
        assertFalse(emptyMsg.hasFixed32S());
        assertFalse(emptyMsg.hasFixed64S());
        assertFalse(emptyMsg.hasSfixed32S());
        assertFalse(emptyMsg.hasSfixed64S());
        assertFalse(emptyMsg.hasSint32S());
        assertFalse(emptyMsg.hasSint64S());
        assertFalse(emptyMsg.hasInt32S());
        assertFalse(emptyMsg.hasInt64S());
        assertFalse(emptyMsg.hasUint32S());
        assertFalse(emptyMsg.hasUint64S());

        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesNonPacked());
        assertNotEquals(msg, emptyMsg);

        assertTrue(msg.hasBools());
        assertTrue(msg.hasDoubles());
        assertTrue(msg.hasFloats());
        assertTrue(msg.hasFixed32S());
        assertTrue(msg.hasFixed64S());
        assertTrue(msg.hasSfixed32S());
        assertTrue(msg.hasSfixed64S());
        assertTrue(msg.hasSint32S());
        assertTrue(msg.hasSint64S());
        assertTrue(msg.hasInt32S());
        assertTrue(msg.hasInt64S());
        assertTrue(msg.hasUint32S());
        assertTrue(msg.hasUint64S());

        assertArrayEquals(new boolean[]{true, false, true, true}, msg.getBools().toArray());
        assertArrayEquals(new double[]{Double.POSITIVE_INFINITY, -2d, 3d, 4d}, msg.getDoubles().toArray(), 0);
        assertArrayEquals(new float[]{10f, 20f, -30f, Float.NaN}, msg.getFloats().toArray(), 0);
        assertArrayEquals(new int[]{2, -2, 4, 67423}, msg.getFixed32S().toArray());
        assertArrayEquals(new long[]{3231313L, 6L, -7L, 8L}, msg.getFixed64S().toArray());
        assertArrayEquals(new int[]{2, -3, 4, 5}, msg.getSfixed32S().toArray());
        assertArrayEquals(new long[]{5L, -6L, 7L, -8L}, msg.getSfixed64S().toArray());
        assertArrayEquals(new int[]{2, -3, 4, 5}, msg.getSint32S().toArray());
        assertArrayEquals(new long[]{5L, 6L, -7L, 8L}, msg.getSint64S().toArray());
        assertArrayEquals(new int[]{2, 3, -4, 5}, msg.getInt32S().toArray());
        assertArrayEquals(new long[]{5L, -6L, 7L, 8L}, msg.getInt64S().toArray());
        assertArrayEquals(new int[]{2, 300, 4, 5}, msg.getUint32S().toArray());
        assertArrayEquals(new long[]{5L, 6L, 23L << 40, 8L}, msg.getUint64S().toArray());

        RepeatedPackables.Packed manualMsg = RepeatedPackables.Packed.newInstance()
                .addAllBools(true, false, true, true)
                .addAllDoubles(Double.POSITIVE_INFINITY, -2d, 3d, 4d)
                .addAllFloats(10f, 20f, -30f, Float.NaN)
                .addAllFixed32S(2, -2, 4, 67423)
                .addAllFixed64S(3231313L, 6L, -7L, 8L)
                .addAllSfixed32S(2, -3, 4, 5)
                .addAllSfixed64S(5L, -6L, 7L, -8L)
                .addAllSint32S(2, -3, 4, 5)
                .addAllSint64S(5L, 6L, -7L, 8L)
                .addAllInt32S(2, 3, -4, 5)
                .addAllInt64S(5L, -6L, 7L, 8L)
                .addAllUint32S(2, 300, 4, 5)
                .addAllUint64S(5L, 6L, 23L << 40, 8L);
        assertEquals(msg, manualMsg);

        // Make sure packed fields can be parsed from non-packed data to maintain forwards compatibility
        byte[] packed = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesPacked()).toByteArray();
        byte[] nonPacked = RepeatedPackables.NonPacked.parseFrom(CompatibilityTest.repeatedPackablesNonPacked()).toByteArray();

        assertEquals(msg, RepeatedPackables.Packed.parseFrom(packed));
        assertEquals(RepeatedPackables.Packed.parseFrom(packed), RepeatedPackables.Packed.parseFrom(nonPacked));
        assertEquals(RepeatedPackables.NonPacked.parseFrom(packed), RepeatedPackables.NonPacked.parseFrom(nonPacked));

    }

    @Test
    public void testOptionalEnums() throws IOException {
        TestAllTypes emptyMsg = TestAllTypes.newInstance();
        assertFalse(emptyMsg.hasOptionalNestedEnum());
        assertFalse(emptyMsg.hasOptionalForeignEnum());
        assertFalse(emptyMsg.hasOptionalImportEnum());

        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.optionalEnums());
        assertNotEquals(msg, emptyMsg);
        assertTrue(msg.hasOptionalNestedEnum());
        assertTrue(msg.hasOptionalForeignEnum());
        assertTrue(msg.hasOptionalImportEnum());

        assertEquals(NestedEnum.FOO, msg.getOptionalNestedEnum());
        assertEquals(ForeignEnum.FOREIGN_BAR, msg.getOptionalForeignEnum());
        assertEquals(ImportEnum.IMPORT_BAZ, msg.getOptionalImportEnum());

        TestAllTypes manualMsg = TestAllTypes.newInstance()
                .setOptionalNestedEnum(NestedEnum.FOO)
                .setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR)
                .setOptionalImportEnum(ImportEnum.IMPORT_BAZ);
        assertEquals(msg, manualMsg);

        try {
            manualMsg.setOptionalNestedEnum(null);
            fail();
        } catch (NullPointerException npe) {
            assertTrue(manualMsg.hasOptionalNestedEnum());
            assertEquals(ForeignEnum.FOREIGN_BAR, msg.getOptionalForeignEnum());
        }

        // Test round-trip
        TestAllTypes msg3 = TestAllTypes.parseFrom(manualMsg.toByteArray());
        assertEquals(msg, msg3);
    }

    @Test
    public void testRepeatedEnums() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedEnums());
        assertEquals(4, msg.getRepeatedNestedEnum().length());
        assertEquals(NestedEnum.FOO, msg.getRepeatedNestedEnum().get(0));
        assertEquals(NestedEnum.BAR, msg.getRepeatedNestedEnum().get(1));
        assertEquals(NestedEnum.BAZ, msg.getRepeatedNestedEnum().get(2));
        assertEquals(NestedEnum.BAZ, msg.getRepeatedNestedEnum().get(3));
        TestAllTypes actual = TestAllTypes.parseFrom(TestAllTypes.newInstance().copyFrom(msg).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();

        // Setter
        assertFalse(msg.hasOptionalString());
        msg.setOptionalString("optionalString\uD83D\uDCA9");
        assertTrue(msg.hasOptionalString());

        // Mutable getter
        assertFalse(msg.hasOptionalCord());
        msg.getMutableOptionalCord()
                .append("he")
                .append("llo!");
        assertTrue(msg.hasOptionalCord());

        // Parse
        TestAllTypes actual = TestAllTypes.parseFrom(msg.toByteArray());
        assertEquals(msg, actual);

        assertEquals("optionalString\uD83D\uDCA9", actual.getOptionalString().toString());
        assertEquals("hello!", actual.getOptionalCord().toString());
    }

    @Test
    public void testRepeatedStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedStrings());
        assertEquals(4, msg.getRepeatedString().length());
        assertEquals("hello", msg.getRepeatedString().get(0).toString());
        assertEquals("world", msg.getRepeatedString().get(1).toString());
        assertEquals("ascii", msg.getRepeatedString().get(2).toString());
        assertEquals("utf8\uD83D\uDCA9", msg.getRepeatedString().get(3).toString());

        TestAllTypes msg2 = TestAllTypes.newInstance();
        msg2.getMutableRepeatedString()
                .copyFrom(msg.getRepeatedString().toArray());

        TestAllTypes actual = TestAllTypes.parseFrom(msg2.toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testBytes() throws IOException {
        byte[] utf8Bytes = "optionalByteString\uD83D\uDCA9".getBytes(UTF_8);
        byte[] randomBytes = new byte[256];
        new Random(0).nextBytes(randomBytes);

        TestAllTypes msg = TestAllTypes.newInstance();

        assertFalse(msg.hasOptionalBytes());
        msg.addAllOptionalBytes(utf8Bytes);
        assertTrue(msg.hasOptionalBytes());
        assertArrayEquals(utf8Bytes, msg.getOptionalBytes().toArray());

        assertFalse(msg.hasDefaultBytes());
        msg.getMutableDefaultBytes()
                .copyFrom(randomBytes);
        assertTrue(msg.hasDefaultBytes());
        assertArrayEquals(randomBytes, msg.getDefaultBytes().toArray());

        // Parse
        TestAllTypes parsedMsg = TestAllTypes.parseFrom(msg.toByteArray());
        assertEquals(msg, parsedMsg);
        assertArrayEquals(utf8Bytes, parsedMsg.getOptionalBytes().toArray());
        assertArrayEquals(randomBytes, parsedMsg.getDefaultBytes().toArray());
    }

    @Test
    public void testRepeatedBytes() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedBytes());
        assertEquals(2, msg.getRepeatedBytes().length());
        assertArrayEquals("ascii".getBytes(UTF_8), msg.getRepeatedBytes().get(0).toArray());
        assertArrayEquals("utf8\uD83D\uDCA9".getBytes(UTF_8), msg.getRepeatedBytes().get(1).toArray());
        TestAllTypes actual = TestAllTypes.parseFrom(TestAllTypes.newInstance().copyFrom(msg).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testOptionalMessages() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();

        // Setter
        assertFalse(msg.hasOptionalNestedMessage());
        msg.getMutableOptionalNestedMessage()
                .setBb(2);
        assertTrue(msg.hasOptionalNestedMessage());

        // Mutable getter
        assertFalse(msg.hasOptionalForeignMessage());
        msg.setOptionalForeignMessage(ForeignMessage.newInstance().setC(3));
        assertTrue(msg.hasOptionalForeignMessage());

        // Compare w/ gen-Java and round-trip parsing
        assertEquals(msg, TestAllTypes.parseFrom(CompatibilityTest.optionalMessages()));
        assertEquals(msg, TestAllTypes.parseFrom(msg.toByteArray()));
    }

    @Test
    public void testRepeatedMessages() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedMessages());
        assertEquals(3, msg.getRepeatedForeignMessage().length());
        assertEquals(ForeignMessage.newInstance().setC(0), msg.getRepeatedForeignMessage().get(0));
        assertEquals(ForeignMessage.newInstance().setC(1), msg.getRepeatedForeignMessage().get(1));
        assertEquals(ForeignMessage.newInstance().setC(2), msg.getRepeatedForeignMessage().get(2));

        TestAllTypes msg2 = TestAllTypes.newInstance()
                .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(0))
                .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(1))
                .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(2));
        assertEquals(msg, msg2);

        TestAllTypes actual = TestAllTypes.parseFrom(TestAllTypes.newInstance().copyFrom(msg2).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testHighFieldNumbers() throws IOException {
        MessageWithMultibyteNumbers expected = MessageWithMultibyteNumbers.newInstance()
                .setTagSize1(1)
                .setTagSize2(2)
                .setTagSize3(3)
                .setTagSize4(4)
                .setTagSize5(5)
                .setTagSizeMax(6);
        MessageWithMultibyteNumbers actual = MessageWithMultibyteNumbers.parseFrom(expected.toByteArray());
        assertEquals(expected, actual);
    }

    @Test
    public void testAllTypesRequired() throws IOException {
        TestAllTypesRequired expected = TestAllTypesRequired.newInstance()
                .setRequiredBool(true)
                .setRequiredDouble(100.0d)
                .setRequiredFloat(101.0f)
                .setRequiredFixed32(102)
                .setRequiredFixed64(103)
                .setRequiredSfixed32(104)
                .setRequiredSfixed64(105)
                .setRequiredSint32(106)
                .setRequiredSint64(107)
                .setRequiredInt32(108)
                .setRequiredInt64(109)
                .setRequiredUint32(110)
                .setRequiredUint64(111)
                .setRequiredString("test")
                .addRequiredBytes((byte) 0)
                .setRequiredNestedEnum(TestAllTypesRequired.NestedEnum.BAR)
                .setRequiredNestedMessage(UnittestRequired.SimpleMessage.newInstance().setRequiredField(0));
        byte[] output = expected.toByteArray();

        TestAllTypesRequired actual = TestAllTypesRequired.parseFrom(expected.toByteArray());
        assertEquals(expected, actual);
        assertArrayEquals(output, actual.toByteArray());

        try {
            expected.clearRequiredBool().toByteArray();
            fail("should not serialize with missing required field");
        } catch (Throwable t) {
        }

        // Check isInitialized()
        assertFalse(expected.isInitialized());
        expected.setRequiredBool(false);
        assertTrue(expected.isInitialized());
        expected.clearRequiredInt32();
        assertFalse(expected.isInitialized());
        expected.setRequiredInt32(108);
        assertTrue(expected.isInitialized());
        expected.getMutableRequiredNestedMessage().clear();
        assertFalse(expected.isInitialized());
        expected.getMutableRequiredNestedMessage().setRequiredField(0);
        assertTrue(expected.isInitialized());

    }

    @Test
    public void testSkipUnknownFields() throws IOException {
        ProtoSource source = ProtoSource.newInstance().wrap(CompatibilityTest.getCombinedMessage());
        TestAllTypes.NestedMessage.newInstance().mergeFrom(source);
        assertTrue(source.isAtEnd());
    }

    @Test
    public void clearFirstBit() throws IOException {
        TestAllTypes.NestedMessage msg = TestAllTypes.NestedMessage.newInstance();
        msg.setBb(1);
        assertTrue(msg.hasBb());
        msg.clearBb();
        assertFalse(msg.hasBb());
    }

    @Test
    public void testRepeatableMessageIterator() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedMessages());
        int sum = 0;
        for (ForeignMessage foreignMessage : msg.getRepeatedForeignMessage()) {
            sum += foreignMessage.getC();
        }
        assertEquals(3, sum);
    }

    @Test
    public void testOneofFields() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        assertFalse(msg.hasOneofField());

        msg.setOneofFixed64(10);
        assertTrue(msg.hasOneofField());
        assertTrue(msg.hasOneofFixed64());
        byte[] fixed64 = msg.toByteArray();

        msg.getMutableOneofNestedMessage().setBb(2);
        assertTrue(msg.hasOneofField());
        assertTrue(msg.hasOneofNestedMessage());
        assertFalse(msg.hasOneofFixed64());
        assertEquals(0, msg.getOneofFixed64());
        byte[] nestedMsg = msg.toByteArray();

        msg.setOneofString("oneOfString");
        assertTrue(msg.hasOneofField());
        assertTrue(msg.hasOneofString());
        assertFalse(msg.hasOneofNestedMessage());
        assertEquals(0, msg.getOneofNestedMessage().getBb());
        byte[] string = msg.toByteArray();

        msg.clearOneofField();
        assertFalse(msg.hasOneofField());
        assertFalse(msg.hasOneofString());
        assertFalse(msg.hasOneofNestedMessage());
        assertEquals("", msg.getOneofString().toString());

        msg.setOneofString("test");
        assertTrue(msg.hasOneofString());
        msg.mergeFrom(ProtoSource.newInstance(fixed64));
        assertTrue(msg.hasOneofFixed64());
        assertFalse(msg.hasOneofString());

        msg.mergeFrom(ProtoSource.newInstance(nestedMsg));
        assertTrue(msg.hasOneofNestedMessage());
        assertFalse(msg.hasOneofFixed64());

        msg.mergeFrom(ProtoSource.newInstance(string));
        assertTrue(msg.hasOneofString());
        assertFalse(msg.hasOneofNestedMessage());

    }

    @Test
    public void testToString() throws IOException {
        // known fields
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        msg.setOptionalDouble(103 + 1E-15);
        assertNotNull(msg.toString());

        // add an unknown field
        final int unknownFieldNumber = 12313;
        int numBytes = ProtoSink.computeDoubleSize(unknownFieldNumber, Double.NaN);
        byte[] unknownBytes = msg.getUnknownBytes().setLength(numBytes).array();
        ProtoSink.newInstance(unknownBytes, 0, numBytes).writeDouble(unknownFieldNumber, Double.NaN);
        assertNotNull(msg.toString());
    }

    @Test
    public void testUnknownFields() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        byte[] expected = msg.clearOptionalInt32().toByteArray(); // one number conflicts
        byte[] actual = TestAllTypes.NestedMessage.parseFrom(expected).toByteArray();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testDelimitedStream() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // Write varint delimited message
        byte[] outData = msg.toByteArray();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtoUtil.writeRawVarint32(outData.length, outputStream);
        outputStream.write(outData);

        // Read varint delimited message
        byte[] result = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(result);
        int length = ProtoUtil.readRawVarint32(inputStream);
        assertEquals(outData.length, length);

        byte[] inData = new byte[length];
        if (inputStream.read(inData) != length) {
            fail();
        }
        assertArrayEquals(outData, inData);
    }

}
