/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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
import protos.test.quickbuf.TestAllTypes;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 07 Sep 2020
 */
public class GsonSourceTest {

    @Test
    public void testManualInput() throws Exception {
        final String json = "{\n" +
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
                "  \"optionalString\": \"optionalString\uD83D\uDCA9\",\n" +
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

        AbstractJsonSource source = new GsonSource(new StringReader(json));
        TestAllTypes msg = TestAllTypes.newInstance().mergeFrom(source);
        assertEquals(json, msg.toString());
    }

    @Test
    public void testNullInput() throws Exception {
        String json = "{optionalNestedMessage=null,repeatedString=null,optionalForeignMessage={},repeatedBytes=[null,null]}";
        AbstractJsonSource source = new GsonSource(new StringReader(json));
        TestAllTypes msg = TestAllTypes.newInstance().mergeFrom(source);
        assertTrue(msg.getOptionalNestedMessage().isEmpty());
        assertTrue(msg.hasRepeatedString());
        assertEquals(0, msg.getRepeatedString().length);
        assertEquals(2, msg.getRepeatedBytes().length);
    }

    @Test
    public void testSpecialNumbers() throws Exception {
        String json = "{\n" +
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
        AbstractJsonSource source = new GsonSource(new StringReader(json));
        TestAllTypes msg = TestAllTypes.newInstance().mergeFrom(source);

        assertArrayEquals(new double[]{
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                0,
                -28.3,
                3E6,
                -0,
                17E-3,
                Double.POSITIVE_INFINITY}, msg.getRepeatedDouble().toArray(), 0);

        assertArrayEquals(new int[]{
                0,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                0,
                1,
                2}, msg.getRepeatedInt32().toArray());

    }

    @Test
    public void testAllMessages() throws Exception {
        for (byte[] bytes : CompatibilityTest.getAllMessages()) {
            testRoundTrip(TestAllTypes.parseFrom(bytes));
        }
    }

    @Test
    public void testCombinedMessage() throws Exception {
        testRoundTrip(TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage()));
    }

    @Test
    public void testEnums() throws Exception {
        testRoundTrip(TestAllTypes.parseFrom(CompatibilityTest.optionalEnums()));
        testRoundTrip(TestAllTypes.parseFrom(CompatibilityTest.repeatedEnums()));
    }

    private void testRoundTrip(ProtoMessage<?> msg) throws Exception {
        testRoundTrip(msg, minimized);
        testRoundTrip(msg, pretty);
    }

    private void testRoundTrip(ProtoMessage<?> msg, JsonSink sink) throws Exception {
        msg.writeTo(sink.clear());
        ProtoMessage<?> msg2 = msg
                .clone()
                .clear()
                .mergeFrom(new GsonSource(new StringReader(sink.toString())));
        assertEquals(msg, msg2);
    }

    private final JsonSink minimized = JsonSink.newInstance();
    private final JsonSink pretty = JsonSink.newPrettyInstance();


}
