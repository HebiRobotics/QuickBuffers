package us.hebi.robobuf;

import org.junit.Test;
import us.hebi.robobuf.robo.AllTypesOuterClass.ForeignEnum;
import us.hebi.robobuf.robo.AllTypesOuterClass.TestAllSupportedTypes;
import us.hebi.robobuf.robo.AllTypesOuterClass.TestAllSupportedTypes.NestedEnum;
import us.hebi.robobuf.robo.RepeatedPackables;
import us.hebi.robobuf.robo.external.ImportEnum;

import java.io.IOException;

import static org.junit.Assert.*;
import static us.hebi.robobuf.InternalNano.*;

/**
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public class MessageTest {

    @Test
    public void testOptionalPrimitives() throws IOException {
        TestAllSupportedTypes emptyMsg = new TestAllSupportedTypes();
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

        TestAllSupportedTypes msg = TestAllSupportedTypes.parseFrom(TestSamples.optionalPrimitives());
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

        TestAllSupportedTypes manualMsg = new TestAllSupportedTypes()
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
        byte[] output = MessageNano.toByteArray(manualMsg);
        TestAllSupportedTypes msg3 = TestAllSupportedTypes.parseFrom(output);
        assertEquals(msg, msg3);

    }

    @Test
    public void testRepeatedPackables() throws IOException {
        RepeatedPackables.Packed emptyMsg = new RepeatedPackables.Packed();
        assertFalse(emptyMsg.hasBools());
        assertFalse(emptyMsg.hasDoubles());
        assertFalse(emptyMsg.hasFloats());
        assertFalse(emptyMsg.hasFixed32s());
        assertFalse(emptyMsg.hasFixed64s());
        assertFalse(emptyMsg.hasSfixed32s());
        assertFalse(emptyMsg.hasSfixed64s());
        assertFalse(emptyMsg.hasSint32s());
        assertFalse(emptyMsg.hasSint64s());
        assertFalse(emptyMsg.hasInt32s());
        assertFalse(emptyMsg.hasInt64s());
        assertFalse(emptyMsg.hasUint32s());
        assertFalse(emptyMsg.hasUint64s());

        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(TestSamples.repeatedPackablesNonPacked());
        assertNotEquals(msg, emptyMsg);

        assertTrue(msg.hasBools());
        assertTrue(msg.hasDoubles());
        assertTrue(msg.hasFloats());
        assertTrue(msg.hasFixed32s());
        assertTrue(msg.hasFixed64s());
        assertTrue(msg.hasSfixed32s());
        assertTrue(msg.hasSfixed64s());
        assertTrue(msg.hasSint32s());
        assertTrue(msg.hasSint64s());
        assertTrue(msg.hasInt32s());
        assertTrue(msg.hasInt64s());
        assertTrue(msg.hasUint32s());
        assertTrue(msg.hasUint64s());

        assertArrayEquals(new boolean[]{true, false, true, true}, msg.getBools().toArray());
        assertArrayEquals(new double[]{Double.POSITIVE_INFINITY, -2d, 3d, 4d}, msg.getDoubles().toArray(), 0);
        assertArrayEquals(new float[]{10f, 20f, -30f, Float.NaN}, msg.getFloats().toArray(), 0);
        assertArrayEquals(new int[]{2, -2, 4, 67423}, msg.getFixed32s().toArray());
        assertArrayEquals(new long[]{3231313L, 6L, -7L, 8L}, msg.getFixed64s().toArray());
        assertArrayEquals(new int[]{2, -3, 4, 5}, msg.getSfixed32s().toArray());
        assertArrayEquals(new long[]{5L, -6L, 7L, -8L}, msg.getSfixed64s().toArray());
        assertArrayEquals(new int[]{2, -3, 4, 5}, msg.getSint32s().toArray());
        assertArrayEquals(new long[]{5L, 6L, -7L, 8L}, msg.getSint64s().toArray());
        assertArrayEquals(new int[]{2, 3, -4, 5}, msg.getInt32s().toArray());
        assertArrayEquals(new long[]{5L, -6L, 7L, 8L}, msg.getInt64s().toArray());
        assertArrayEquals(new int[]{2, 300, 4, 5}, msg.getUint32s().toArray());
        assertArrayEquals(new long[]{5L, 6L, 23L << 40, 8L}, msg.getUint64s().toArray());

        // Make sure packed fields can be parsed from non-packed data to maintain forwards compatibility
        byte[] packed = MessageNano.toByteArray(RepeatedPackables.Packed.parseFrom(TestSamples.repeatedPackablesPacked()));
        byte[] nonPacked = MessageNano.toByteArray(RepeatedPackables.NonPacked.parseFrom(TestSamples.repeatedPackablesNonPacked()));

        assertEquals(msg, RepeatedPackables.Packed.parseFrom(packed));
        assertEquals(RepeatedPackables.Packed.parseFrom(packed), RepeatedPackables.Packed.parseFrom(nonPacked));
        assertEquals(RepeatedPackables.NonPacked.parseFrom(packed), RepeatedPackables.NonPacked.parseFrom(nonPacked));

    }

    @Test
    public void testOptionalEnums() throws IOException {
        TestAllSupportedTypes emptyMsg = new TestAllSupportedTypes();
        assertFalse(emptyMsg.hasOptionalNestedEnum());
        assertFalse(emptyMsg.hasOptionalForeignEnum());
        assertFalse(emptyMsg.hasOptionalImportEnum());

        TestAllSupportedTypes msg = TestAllSupportedTypes.parseFrom(TestSamples.optionalEnums());
        assertNotEquals(msg, emptyMsg);
        assertTrue(msg.hasOptionalNestedEnum());
        assertTrue(msg.hasOptionalForeignEnum());
        assertTrue(msg.hasOptionalImportEnum());

        assertEquals(NestedEnum.FOO, msg.getOptionalNestedEnum());
        assertEquals(ForeignEnum.FOREIGN_BAR, msg.getOptionalForeignEnum());
        assertEquals(ImportEnum.IMPORT_BAZ, msg.getOptionalImportEnum());

        TestAllSupportedTypes manualMsg = new TestAllSupportedTypes()
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
        byte[] output = MessageNano.toByteArray(manualMsg);
        TestAllSupportedTypes msg3 = TestAllSupportedTypes.parseFrom(output);
        assertEquals(msg, msg3);
    }

    @Test
    public void testDefaults() throws IOException {
        TestAllSupportedTypes msg = new TestAllSupportedTypes();
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
            assertEquals("world", new String(msg.getDefaultBytesArray(), 0, msg.getDefaultBytesLength(), UTF_8));
            assertEquals("dünya", msg.getDefaultStringNonascii().toString());
            assertEquals("dünyab", new String(msg.getDefaultBytesNonasciiArray(), 0, msg.getDefaultBytesNonasciiLength(), UTF_8));
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
            byte[] result = MessageNano.toByteArray(msg);
            int msgSerializedSize = msg.getSerializedSize();
            assertEquals(3, msgSerializedSize);
            assertEquals(result.length, msgSerializedSize);
            msg.clear();
        }
    }

    @Test
    public void testStrings() throws IOException {



    }

    @Test(expected = IllegalStateException.class)
    public void testMissingRequiredField() {
        MessageNano.toByteArray(new TestAllSupportedTypes());
    }

}
