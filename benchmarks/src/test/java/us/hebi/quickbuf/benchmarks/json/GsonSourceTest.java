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

package us.hebi.quickbuf.benchmarks.json;

import com.google.gson.stream.JsonReader;
import org.junit.Ignore;
import org.junit.Test;
import protos.test.quickbuf.TestAllTypes;
import us.hebi.quickbuf.AbstractJsonSource;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 07 Sep 2020
 */
public class GsonSourceTest {

    @Ignore
    @Test
    public void testGsonAPI() throws IOException {
        JsonReader reader = new JsonReader(new StringReader(json));

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            System.out.println(key);
            Object value = null;
            switch (key) {
                case "optionalDouble":
                    value = reader.nextDouble();
                    break;
                case "optionalBool":
                    value = reader.nextBoolean();
                    break;
                case "optionalNestedMessage":
                case "optionalForeignMessage":
                case "optionalImportMessage":
                    reader.skipValue();
                    break;
                default:
                    reader.skipValue();
                    value = null;
            }

            System.out.println(value);
            System.out.println();
        }
        reader.endObject();


    }

    @Test
    public void testGsonWrapper() throws IOException {
        AbstractJsonSource source = new GsonSource(new StringReader(json));
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.mergeFrom(source);
        System.out.println(msg);
        assertEquals(json, msg.toString());
    }

    private final String json = "{\n" +
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

}
