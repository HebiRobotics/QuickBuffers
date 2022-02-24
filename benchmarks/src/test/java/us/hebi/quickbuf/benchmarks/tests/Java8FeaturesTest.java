/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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

package us.hebi.quickbuf.benchmarks.tests;

import org.junit.Test;
import protos.test.quickbuf.ForeignEnum;
import protos.test.quickbuf.RepeatedPackables;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.external.ImportEnum;

import static org.junit.Assert.*;

/**
 * Tests for features that require Java 8 and produce IDE problems when added to
 * the Runtime tests due to the Java 6 target. (IntelliJ doesn't recognize the
 * maven.compiler.testSource property)
 *
 * @author Florian Enner
 * @since 01 Sep 2020
 */
public class Java8FeaturesTest {

    @Test
    public void testTryGetOptionalPrimitives() {
        TestAllTypes msg = TestAllTypes.newInstance();
        assertFalse(msg.tryGetOptionalBool().isPresent());
        assertFalse(msg.tryGetOptionalDouble().isPresent());
        assertFalse(msg.tryGetOptionalFloat().isPresent());
        assertFalse(msg.tryGetOptionalFixed32().isPresent());
        assertFalse(msg.tryGetOptionalFixed64().isPresent());
        assertFalse(msg.tryGetOptionalSfixed32().isPresent());
        assertFalse(msg.tryGetOptionalSfixed64().isPresent());
        assertFalse(msg.tryGetOptionalSint32().isPresent());
        assertFalse(msg.tryGetOptionalSint64().isPresent());
        assertFalse(msg.tryGetOptionalInt32().isPresent());
        assertFalse(msg.tryGetOptionalInt64().isPresent());
        assertFalse(msg.tryGetOptionalUint32().isPresent());
        assertFalse(msg.tryGetOptionalUint64().isPresent());

        msg
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

        assertTrue(msg.tryGetOptionalBool().isPresent());
        assertTrue(msg.tryGetOptionalDouble().isPresent());
        assertTrue(msg.tryGetOptionalFloat().isPresent());
        assertTrue(msg.tryGetOptionalFixed32().isPresent());
        assertTrue(msg.tryGetOptionalFixed64().isPresent());
        assertTrue(msg.tryGetOptionalSfixed32().isPresent());
        assertTrue(msg.tryGetOptionalSfixed64().isPresent());
        assertTrue(msg.tryGetOptionalSint32().isPresent());
        assertTrue(msg.tryGetOptionalSint64().isPresent());
        assertTrue(msg.tryGetOptionalInt32().isPresent());
        assertTrue(msg.tryGetOptionalInt64().isPresent());
        assertTrue(msg.tryGetOptionalUint32().isPresent());
        assertTrue(msg.tryGetOptionalUint64().isPresent());

        assertTrue(msg.getOptionalBool());
        assertEquals(100.0d, msg.tryGetOptionalDouble().getAsDouble(), 0);
        assertEquals(101.0f, msg.tryGetOptionalFloat().getAsDouble(), 0);
        assertEquals(102, msg.tryGetOptionalFixed32().getAsInt());
        assertEquals(103, msg.tryGetOptionalFixed64().getAsLong());
        assertEquals(104, msg.tryGetOptionalSfixed32().getAsInt());
        assertEquals(105, msg.tryGetOptionalSfixed64().getAsLong());
        assertEquals(106, msg.tryGetOptionalSint32().getAsInt());
        assertEquals(107, msg.tryGetOptionalSint64().getAsLong());
        assertEquals(108, msg.tryGetOptionalInt32().getAsInt());
        assertEquals(109, msg.tryGetOptionalInt64().getAsLong());
        assertEquals(110, msg.tryGetOptionalUint32().getAsInt());
        assertEquals(111, msg.tryGetOptionalUint64().getAsLong());

    }

    @Test
    public void testTryGetRepeatedPrimitives() {
        RepeatedPackables.Packed msg = RepeatedPackables.Packed.newInstance()
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

        // (no presence throws error)
        assertTrue(msg.tryGetBools().get().get(0));
        assertEquals(Double.POSITIVE_INFINITY, msg.tryGetDoubles().get().get(0), 0);
        assertEquals(10f, msg.tryGetFloats().get().get(0), 0);
        assertEquals(2, msg.tryGetFixed32S().get().get(0));
        assertEquals(3231313L, msg.tryGetFixed64S().get().get(0));
        assertEquals(2, msg.tryGetSfixed32S().get().get(0));
        assertEquals(5L, msg.tryGetSfixed64S().get().get(0));
        assertEquals(2, msg.tryGetSint32S().get().get(0));
        assertEquals(5L, msg.tryGetSint64S().get().get(0));
        assertEquals(2, msg.tryGetInt32S().get().get(0));
        assertEquals(5L, msg.tryGetInt64S().get().get(0));
        assertEquals(2, msg.tryGetUint32S().get().get(0));
        assertEquals(5L, msg.tryGetUint64S().get().get(0));

    }

    @Test
    public void testTryGetEnums() {
        TestAllTypes msg = TestAllTypes.newInstance();
        assertFalse(msg.tryGetOptionalNestedEnum().isPresent());
        assertFalse(msg.tryGetOptionalForeignEnum().isPresent());
        assertFalse(msg.tryGetOptionalImportEnum().isPresent());

        msg
                .setOptionalNestedEnum(TestAllTypes.NestedEnum.FOO)
                .setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR)
                .setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        assertEquals(TestAllTypes.NestedEnum.FOO, msg.tryGetOptionalNestedEnum().get());
        assertEquals(ForeignEnum.FOREIGN_BAR, msg.tryGetOptionalForeignEnum().get());
        assertEquals(ImportEnum.IMPORT_BAZ, msg.tryGetOptionalImportEnum().get());

        // Repeated
        assertEquals(ForeignEnum.FOREIGN_BAR, TestAllTypes.newInstance()
                .addRepeatedForeignEnum(ForeignEnum.FOREIGN_BAR)
                .tryGetRepeatedForeignEnum()
                .get()
                .get(0));

    }

    @Test
    public void testTryGetStrings() {
        String string = "hello optional";
        assertEquals(string, TestAllTypes.newInstance()
                .setOptionalString(string)
                .tryGetOptionalString()
                .get());

        // repeated
        assertEquals(string, TestAllTypes.newInstance()
                .addRepeatedString(string)
                .tryGetRepeatedString()
                .get()
                .get(0));
    }

    @Test
    public void testTryMessages() {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOneofNestedMessage().setBb(1);
        msg.getMutableRepeatedNestedMessage().next().setBb(2);
        assertEquals(1, msg.tryGetOneofNestedMessage().get().getBb());
        assertEquals(2, msg.tryGetRepeatedNestedMessage().get().get(0).getBb());
    }

}
