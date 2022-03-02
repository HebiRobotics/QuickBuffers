/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2022 HEBI Robotics
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

package us.hebi.quickbuf.compat;

import org.junit.Test;
import protos.test.quickbuf.ForeignEnum;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.external.ImportEnum;
import us.hebi.quickbuf.CompatibilityTest;
import us.hebi.quickbuf.FieldName;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.RepeatedFloat;

import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 25 Feb 2022
 */
public class GsonSinkTest {

    @Test
    public void testRoundTrip() throws IOException {
        TestAllTypes expected = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        TestAllTypes actual = TestAllTypes.newInstance();

        String json = GsonSink.newStringWriter().setWriteEnumsAsInts(false).writeMessage(expected).toString();
        actual.clear().mergeFrom(new GsonSource(new StringReader(json)));
        assertEquals(expected, actual);

        json = GsonSink.newStringWriter().setWriteEnumsAsInts(true).writeMessage(expected).toString();
        actual.clear().mergeFrom(new GsonSource(new StringReader(json)));
        assertEquals(expected, actual);
    }

    @Test
    public void testEnumNumbers() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.setOptionalNestedEnumValue(TestAllTypes.NestedEnum.BAR_VALUE);
        msg.setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR);
        msg.setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        String desired = "{\"optionalNestedEnum\":2,\"optionalForeignEnum\":5,\"optionalImportEnum\":9}";
        String result = GsonSink.newStringWriter().setWriteEnumsAsInts(true).writeMessage(msg).toString();
        assertEquals(desired, result);
    }

    @Test
    public void testEnumStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.setOptionalNestedEnumValue(TestAllTypes.NestedEnum.BAR_VALUE);
        msg.setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR);
        msg.setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        String desired = "{\"optionalNestedEnum\":\"BAR\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\"}";
        String result = GsonSink.newStringWriter().setWriteEnumsAsInts(false).writeMessage(msg).toString();
        assertEquals(desired, result);
    }

    @Test
    public void testRepeatedEnums() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableRepeatedNestedEnum().addAll(TestAllTypes.NestedEnum.FOO, TestAllTypes.NestedEnum.BAR, TestAllTypes.NestedEnum.BAZ, TestAllTypes.NestedEnum.BAZ);

        assertEquals("{\"repeatedNestedEnum\":[\"FOO\",\"BAR\",\"BAZ\",\"BAZ\"]}",
                GsonSink.newStringWriter().setWriteEnumsAsInts(false).writeMessage(msg).toString());

        assertEquals("{\"repeatedNestedEnum\":[1,2,3,3]}",
                GsonSink.newStringWriter().setWriteEnumsAsInts(true).writeMessage(msg).toString());
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
        assertEquals(desired, GsonSink.newStringWriter().writeMessage(msg).toString());
    }

    @Test
    public void testNestedMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalNestedMessage().setBb(2); // with content
        msg.getMutableOptionalForeignMessage(); // empty

        // minimal
        assertEquals("{\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{}}",
                GsonSink.newStringWriter().writeMessage(msg).toString());

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
        assertEquals("{}", GsonSink.newStringWriter().writeMessage(msg).toString());
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
        String desired = "{\"optionalDouble\":100.0,\"optionalFixed64\":103,\"optionalSfixed64\":105,\"optionalInt64\":109,\"optionalUint64\":111,\"optionalSint64\":107,\"optionalFloat\":101.0,\"optionalFixed32\":102,\"optionalSfixed32\":104,\"optionalInt32\":108,\"optionalUint32\":110,\"optionalSint32\":106,\"optionalNestedEnum\":\"FOO\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\",\"optionalBool\":true,\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{\"c\":3},\"optionalImportMessage\":{},\"optionalBytes\":\"dXRmOPCfkqk=\",\"defaultBytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optionalString\":\"optionalString\uD83D\uDCA9\",\"optionalCord\":\"hello!\",\"repeatedDouble\":[\"NaN\",\"-Infinity\",0.0,-28.3],\"repeatedFloat\":[],\"repeatedInt32\":[-2,-1,0,1,2,3,4,5],\"repeatedPackedInt32\":[-1,0,1,2,3,4,5],\"repeatedForeignMessage\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeatedBytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeatedString\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";
        assertEquals(desired, GsonSink.newStringWriter().setWriteEnumsAsInts(false).writeMessage(msg).toString());
    }

    @Test
    public void testMiniOutputProtoNames() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // add some empty messages and arrays
        msg.getMutableOptionalImportMessage();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedBytes().next();
        msg.getMutableRepeatedDouble().addAll(new double[]{Double.NaN, Double.NEGATIVE_INFINITY, 0.0, -28.3d});
        msg.getMutableRepeatedFloat().clear();

        // Copied from https://codebeautify.org/jsonminifier
        String desired = "{\"optional_double\":100.0,\"optional_fixed64\":103,\"optional_sfixed64\":105,\"optional_int64\":109,\"optional_uint64\":111,\"optional_sint64\":107,\"optional_float\":101.0,\"optional_fixed32\":102,\"optional_sfixed32\":104,\"optional_int32\":108,\"optional_uint32\":110,\"optional_sint32\":106,\"optional_nested_enum\":\"FOO\",\"optional_foreign_enum\":\"FOREIGN_BAR\",\"optional_import_enum\":\"IMPORT_BAZ\",\"optional_bool\":true,\"optional_nested_message\":{\"bb\":2},\"optional_foreign_message\":{\"c\":3},\"optional_import_message\":{},\"optional_bytes\":\"dXRmOPCfkqk=\",\"default_bytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optional_string\":\"optionalString\uD83D\uDCA9\",\"optional_cord\":\"hello!\",\"repeated_double\":[\"NaN\",\"-Infinity\",0.0,-28.3],\"repeated_float\":[],\"repeated_int32\":[-2,-1,0,1,2,3,4,5],\"repeated_packed_int32\":[-1,0,1,2,3,4,5],\"repeated_foreign_message\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeated_bytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeated_string\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";
        assertEquals(desired, GsonSink.newStringWriter().setPreserveProtoFieldNames(true).setWriteEnumsAsInts(false).writeMessage(msg).toString());
    }

    @Test
    public void testRepeatedFloat() throws IOException {
        RepeatedFloat floats = RepeatedFloat.newEmptyInstance();
        for (int i = -4; i < 4; i++) {
            floats.add(i / 2f);
        }
        FieldName field = FieldName.forField("data");
        String result = GsonSink.newStringWriter()
                .beginObject()
                .writeRepeatedFloat(field, floats)
                .endObject()
                .toString();
        assertEquals("{\"data\":[-2.0,-1.5,-1.0,-0.5,0.0,0.5,1.0,1.5]}", result);
    }

}
