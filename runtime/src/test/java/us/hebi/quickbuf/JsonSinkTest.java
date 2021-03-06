/*-
 * #%L
 * quickbuf-runtime
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
import protos.test.quickbuf.ForeignEnum;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.TestAllTypes.NestedEnum;
import protos.test.quickbuf.external.ImportEnum;

import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 13 Jul 2020
 */
public class JsonSinkTest {

    @Test
    public void reserve() {
    }

    @Test
    public void testEnumNumbers() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.setOptionalNestedEnumValue(NestedEnum.BAR_VALUE);
        msg.setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR);
        msg.setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        String desired = "{\"optionalNestedEnum\":2,\"optionalForeignEnum\":5,\"optionalImportEnum\":9}";
        String result = JsonSink.newInstance().setWriteEnumStrings(false).writeMessage(msg).toString();
        assertEquals(desired, result);
    }

    @Test
    public void testEnumStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.setOptionalNestedEnumValue(NestedEnum.BAR_VALUE);
        msg.setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR);
        msg.setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        String desired = "{\"optionalNestedEnum\":\"BAR\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\"}";
        String result = JsonSink.newInstance().setWriteEnumStrings(true).writeMessage(msg).toString();
        assertEquals(desired, result);
    }

    @Test
    public void testRepeatedEnums() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableRepeatedNestedEnum().addAll(NestedEnum.FOO, NestedEnum.BAR, NestedEnum.BAZ, NestedEnum.BAZ);

        assertEquals("{\"repeatedNestedEnum\":[\"FOO\",\"BAR\",\"BAZ\",\"BAZ\"]}",
                JsonSink.newInstance().setWriteEnumStrings(true).writeMessage(msg).toString());

        assertEquals("{\"repeatedNestedEnum\":[1,2,3,3]}",
                JsonSink.newInstance().setWriteEnumStrings(false).writeMessage(msg).toString());
    }

    @Test
    public void testBytes() throws IOException {
        byte[] randomBytes = new byte[31];
        new Random(0).nextBytes(randomBytes);
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalBytes().addAll(randomBytes);
        msg.getMutableRepeatedBytes().add(new byte[0]);
        msg.getMutableRepeatedBytes().add(new byte[]{'A'});
        msg.getMutableRepeatedBytes().add(new byte[]{'A', 'B'});
        msg.getMutableRepeatedBytes().add(new byte[]{'A', 'B', 'C'});
        msg.getMutableRepeatedBytes().add(randomBytes);

        String javaBase64 = Base64.getEncoder().encodeToString(randomBytes);
        String desired = String.format(
                "{\"optionalBytes\":\"%s\",\"repeatedBytes\":[\"\",\"QQ==\",\"QUI=\",\"QUJD\",\"%s\"]}",
                javaBase64, javaBase64);
        assertEquals(desired, JsonSink.newInstance().writeMessage(msg).toString());
    }

    @Test
    public void testNestedMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalNestedMessage().setBb(2); // with content
        msg.getMutableOptionalForeignMessage(); // empty

        // minimal
        assertEquals("{\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{}}",
                JsonSink.newInstance().writeMessage(msg).toString());

        // pretty print
        assertEquals("{\n" +
                "  \"optionalNestedMessage\": {\n" +
                "    \"bb\": 2\n" +
                "  },\n" +
                "  \"optionalForeignMessage\": {}\n" +
                "}", msg.toString());
    }

    @Test
    public void testEmptyMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        assertEquals("{}", JsonSink.newInstance().writeMessage(msg).toString());
        assertEquals("{}", JsonSink.newPrettyInstance().writeMessage(msg).toString());
    }

    @Test
    public void testPrettyOutput() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // add some empty messages and arrays
        msg.getMutableOptionalImportMessage();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedBytes().next();
        msg.getMutableRepeatedDouble().addAll(new double[]{Double.NaN, Double.NEGATIVE_INFINITY, 0.0, -28.3d});
        msg.getMutableRepeatedFloat().clear();

        // Copied from https://jsonformatter.org/json-parser
        String desired = "{\n" +
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
        assertEquals(desired, msg.toString());
    }

    @Test
    public void testMiniOutput() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // add some empty messages and arrays
        msg.getMutableOptionalImportMessage();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedBytes().next();
        msg.getMutableRepeatedDouble().addAll(new double[]{Double.NaN, Double.NEGATIVE_INFINITY, 0.0, -28.3d});
        msg.getMutableRepeatedFloat().clear();

        // Copied from https://codebeautify.org/jsonminifier
        String desired = "{\"optionalDouble\":100,\"optionalFixed64\":103,\"optionalSfixed64\":105,\"optionalInt64\":109,\"optionalUint64\":111,\"optionalSint64\":107,\"optionalFloat\":101,\"optionalFixed32\":102,\"optionalSfixed32\":104,\"optionalInt32\":108,\"optionalUint32\":110,\"optionalSint32\":106,\"optionalNestedEnum\":\"FOO\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\",\"optionalBool\":true,\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{\"c\":3},\"optionalImportMessage\":{},\"optionalBytes\":\"dXRmOPCfkqk=\",\"defaultBytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optionalString\":\"optionalString\uD83D\uDCA9\",\"optionalCord\":\"hello!\",\"repeatedDouble\":[\"NaN\",\"-Infinity\",0,-28.3],\"repeatedFloat\":[],\"repeatedInt32\":[-2,-1,0,1,2,3,4,5],\"repeatedPackedInt32\":[-1,0,1,2,3,4,5],\"repeatedForeignMessage\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeatedBytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeatedString\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";
        assertEquals(desired, JsonSink.newInstance().setWriteEnumStrings(true).writeMessage(msg).toString());
    }

    @Test
    public void testRepeatedFloat() throws IOException {
        RepeatedFloat floats = RepeatedFloat.newEmptyInstance();
        for (int i = -4; i < 4; i++) {
            floats.add(i / 2f);
        }
        FieldName field = FieldName.forField("data");
        String result = JsonSink.newInstance()
                .beginObject()
                .writeRepeatedFloat(field, floats)
                .endObject()
                .toString();
        assertEquals("{\"data\":[-2,-1.5,-1,-0.5,0,0.5,1,1.5]}", result);
    }

}
