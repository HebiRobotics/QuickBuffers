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

import org.junit.Assert;
import org.junit.Test;
import protos.test.quickbuf.TestAllTypes;
import us.hebi.quickbuf.CompatibilityTest;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.JsonSinkTest;
import us.hebi.quickbuf.JsonSource;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author Florian Enner
 * @since 25 Feb 2022
 */
public class GsonSinkTest extends JsonSinkTest {

    public GsonSinkTest() {
        // Note: Gson/Jackson always serialize doubles with a comma and at least one digit
        miniOutputResult = "{\"optionalDouble\":100.0,\"optionalFixed64\":103,\"optionalSfixed64\":105,\"optionalInt64\":109,\"optionalUint64\":111,\"optionalSint64\":107,\"optionalFloat\":101.0,\"optionalFixed32\":102,\"optionalSfixed32\":104,\"optionalInt32\":108,\"optionalUint32\":110,\"optionalSint32\":106,\"optionalNestedEnum\":\"FOO\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\",\"optionalBool\":true,\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{\"c\":3},\"optionalImportMessage\":{},\"optionalgroup\":{\"a\":4},\"optionalBytes\":\"dXRmOPCfkqk=\",\"defaultBytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optionalString\":\"optionalString\uD83D\uDCA9\",\"optionalCord\":\"hello!\",\"repeatedDouble\":[\"NaN\",\"-Infinity\",0.0,-28.3],\"repeatedFloat\":[],\"repeatedInt32\":[-2,-1,0,1,2,3,4,5],\"repeatedPackedInt32\":[-1,0,1,2,3,4,5],\"repeatedForeignMessage\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeatedgroup\":[{\"a\":3},{\"a\":4}],\"repeatedBytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeatedString\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";
        miniOutputProtoNamesResult = "{\"optional_double\":100.0,\"optional_fixed64\":103,\"optional_sfixed64\":105,\"optional_int64\":109,\"optional_uint64\":111,\"optional_sint64\":107,\"optional_float\":101.0,\"optional_fixed32\":102,\"optional_sfixed32\":104,\"optional_int32\":108,\"optional_uint32\":110,\"optional_sint32\":106,\"optional_nested_enum\":\"FOO\",\"optional_foreign_enum\":\"FOREIGN_BAR\",\"optional_import_enum\":\"IMPORT_BAZ\",\"optional_bool\":true,\"optional_nested_message\":{\"bb\":2},\"optional_foreign_message\":{\"c\":3},\"optional_import_message\":{},\"optionalgroup\":{\"a\":4},\"optional_bytes\":\"dXRmOPCfkqk=\",\"default_bytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optional_string\":\"optionalString\uD83D\uDCA9\",\"optional_cord\":\"hello!\",\"repeated_double\":[\"NaN\",\"-Infinity\",0.0,-28.3],\"repeated_float\":[],\"repeated_int32\":[-2,-1,0,1,2,3,4,5],\"repeated_packed_int32\":[-1,0,1,2,3,4,5],\"repeated_foreign_message\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeatedgroup\":[{\"a\":3},{\"a\":4}],\"repeated_bytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeated_string\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";
        repeatedFloatResult = "{\"data\":[-2.0,-1.5,-1.0,-0.5,0.0,0.5,1.0,1.5]}";
    }

    @Test
    public void testRoundTrip() throws IOException {
        TestAllTypes expected = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        TestAllTypes actual = TestAllTypes.newInstance();

        String json = newJsonSink().setWriteEnumsAsInts(false).writeMessage(expected).toString();
        actual.clear().mergeFrom(newJsonSource(json));
        Assert.assertEquals(expected, actual);

        json = newJsonSink().setWriteEnumsAsInts(true).writeMessage(expected).toString();
        actual.clear().mergeFrom(newJsonSource(json));
        Assert.assertEquals(expected, actual);
    }

    public JsonSink newJsonSink() {
        return GsonSink.newStringWriter();
    }

    public JsonSource newJsonSource(String json) throws IOException {
        return new GsonSource(new StringReader(json));
    }

}
