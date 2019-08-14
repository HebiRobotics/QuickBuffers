package us.hebi.robobuf;

import org.junit.Test;
import us.hebi.robobuf.robo.ForeignEnum;
import us.hebi.robobuf.robo.ForeignMessage;
import us.hebi.robobuf.robo.RepeatedPackables;
import us.hebi.robobuf.robo.TestAllTypes;
import us.hebi.robobuf.robo.TestAllTypes.NestedEnum;
import us.hebi.robobuf.robo.external.ImportEnum;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.robobuf.InternalNano.*;

/**
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public class MessageTest {

    @Test
    public void testDefaults() throws IOException {
        TestAllTypes msg = new TestAllTypes();
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

            msg.setId(0); // required
            byte[] result = msg.toByteArray();
            int msgSerializedSize = msg.getSerializedSize();
            assertEquals(3, msgSerializedSize);
            assertEquals(result.length, msgSerializedSize);
            msg.clear();
        }
    }

    @Test
    public void testOptionalPrimitives() throws IOException {
        TestAllTypes emptyMsg = new TestAllTypes();
        assertFalse(emptyMsg.hasId());
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

        TestAllTypes msg = TestAllTypes.parseFrom(TestSamples.optionalPrimitives());
        assertNotEquals(msg, emptyMsg);

        assertTrue(msg.hasId());
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
        assertEquals(99, msg.getId());
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

        TestAllTypes manualMsg = new TestAllTypes()
                .setId(99)
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

    }

    @Test
    public void testRepeatedPrimitives() throws IOException {
        RepeatedPackables.Packed emptyMsg = new RepeatedPackables.Packed();
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

        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(TestSamples.repeatedPackablesNonPacked());
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

        RepeatedPackables.Packed manualMsg = new RepeatedPackables.Packed()
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
        byte[] packed = RepeatedPackables.Packed.parseFrom(TestSamples.repeatedPackablesPacked()).toByteArray();
        byte[] nonPacked = RepeatedPackables.NonPacked.parseFrom(TestSamples.repeatedPackablesNonPacked()).toByteArray();

        assertEquals(msg, RepeatedPackables.Packed.parseFrom(packed));
        assertEquals(RepeatedPackables.Packed.parseFrom(packed), RepeatedPackables.Packed.parseFrom(nonPacked));
        assertEquals(RepeatedPackables.NonPacked.parseFrom(packed), RepeatedPackables.NonPacked.parseFrom(nonPacked));

    }

    @Test
    public void testOptionalEnums() throws IOException {
        TestAllTypes emptyMsg = new TestAllTypes();
        assertFalse(emptyMsg.hasOptionalNestedEnum());
        assertFalse(emptyMsg.hasOptionalForeignEnum());
        assertFalse(emptyMsg.hasOptionalImportEnum());

        TestAllTypes msg = TestAllTypes.parseFrom(TestSamples.optionalEnums());
        assertNotEquals(msg, emptyMsg);
        assertTrue(msg.hasOptionalNestedEnum());
        assertTrue(msg.hasOptionalForeignEnum());
        assertTrue(msg.hasOptionalImportEnum());

        assertEquals(NestedEnum.FOO, msg.getOptionalNestedEnum());
        assertEquals(ForeignEnum.FOREIGN_BAR, msg.getOptionalForeignEnum());
        assertEquals(ImportEnum.IMPORT_BAZ, msg.getOptionalImportEnum());

        TestAllTypes manualMsg = new TestAllTypes()
                .setId(0)
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
        TestAllTypes msg = TestAllTypes.parseFrom(TestSamples.repeatedEnums());
        assertEquals(4, msg.getRepeatedNestedEnumCount());
        assertEquals(NestedEnum.FOO, msg.getRepeatedNestedEnum(0));
        assertEquals(NestedEnum.BAR, msg.getRepeatedNestedEnum(1));
        assertEquals(NestedEnum.BAZ, msg.getRepeatedNestedEnum(2));
        assertEquals(NestedEnum.BAZ, msg.getRepeatedNestedEnum(3));
        TestAllTypes actual = TestAllTypes.parseFrom(new TestAllTypes().copyFrom(msg).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testStrings() throws IOException {
        TestAllTypes msg = new TestAllTypes().setId(0);

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
        TestAllTypes msg = TestAllTypes.parseFrom(TestSamples.repeatedStrings());
        assertEquals(4, msg.getRepeatedString().length());
        assertEquals("hello", msg.getRepeatedString().get(0).toString());
        assertEquals("world", msg.getRepeatedString().get(1).toString());
        assertEquals("ascii", msg.getRepeatedString().get(2).toString());
        assertEquals("utf8\uD83D\uDCA9", msg.getRepeatedString().get(3).toString());

        TestAllTypes msg2 = new TestAllTypes();
        msg2.setId(0)
                .getMutableRepeatedString()
                .copyFrom(msg.getRepeatedString().toArray());

        TestAllTypes actual = TestAllTypes.parseFrom(msg2.toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testBytes() throws IOException {
        byte[] utf8Bytes = "optionalByteString\uD83D\uDCA9".getBytes(UTF_8);
        byte[] randomBytes = new byte[256];
        new Random(0).nextBytes(randomBytes);

        TestAllTypes msg = new TestAllTypes().setId(0);

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
        TestAllTypes msg = TestAllTypes.parseFrom(TestSamples.repeatedBytes());
        assertEquals(2, msg.getRepeatedBytes().length());
        assertArrayEquals("ascii".getBytes(UTF_8), msg.getRepeatedBytes().get(0).toArray());
        assertArrayEquals("utf8\uD83D\uDCA9".getBytes(UTF_8), msg.getRepeatedBytes().get(1).toArray());
        TestAllTypes actual = TestAllTypes.parseFrom(new TestAllTypes().copyFrom(msg).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testOptionalMessages() throws IOException {
        TestAllTypes msg = new TestAllTypes().setId(0);

        // Setter
        assertFalse(msg.hasOptionalNestedMessage());
        msg.getMutableOptionalNestedMessage()
                .setBb(2);
        assertTrue(msg.hasOptionalNestedMessage());

        // Mutable getter
        assertFalse(msg.hasOptionalForeignMessage());
        msg.setOptionalForeignMessage(new ForeignMessage().setC(3));
        assertTrue(msg.hasOptionalForeignMessage());

        // Compare w/ gen-Java and round-trip parsing
        assertEquals(msg, TestAllTypes.parseFrom(TestSamples.optionalMessages()));
        assertEquals(msg, TestAllTypes.parseFrom(msg.toByteArray()));
    }

    @Test
    public void testRepeatedMessages() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(TestSamples.repeatedMessages());
        assertEquals(3, msg.getRepeatedForeignMessage().length());
        assertEquals(new ForeignMessage().setC(0), msg.getRepeatedForeignMessage().get(0));
        assertEquals(new ForeignMessage().setC(1), msg.getRepeatedForeignMessage().get(1));
        assertEquals(new ForeignMessage().setC(2), msg.getRepeatedForeignMessage().get(2));

        TestAllTypes msg2 = new TestAllTypes().setId(0)
                .addRepeatedForeignMessage(new ForeignMessage().setC(0))
                .addRepeatedForeignMessage(new ForeignMessage().setC(1))
                .addRepeatedForeignMessage(new ForeignMessage().setC(2));
        assertEquals(msg, msg2);

        TestAllTypes actual = TestAllTypes.parseFrom(new TestAllTypes().copyFrom(msg2).toByteArray());
        assertEquals(msg, actual);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingRequiredField() {
        new TestAllTypes().toByteArray();
    }

}
