/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2026 HEBI Robotics
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

import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Compares the number conversions of the JsonReader and JsonSource against the JDK
 *
 * @author Florian Enner
 * @since 30 Sep 2026
 */
public class JsonNumbersTest {

    @Test
    public void testExactDoubles() throws IOException {
        for (String text : new String[]{"0", "-0", "0.0", "-0.0e5", "0e999999999", "1", "-1.5", "0.000123", "123e30",
                "9007199254740992", "9007199254740993", "1e22", "1e23", "4.35", "0.30000000000000004"}) {
            assertDouble(text);
        }
        Random rnd = new Random(0);
        for (int i = 0; i < 100000; i++) {
            long mantissa = rnd.nextLong() >>> (10 + rnd.nextInt(54));
            assertDouble(mantissa + "e" + (rnd.nextInt(60) - 22));
        }
    }

    private static void assertDouble(String text) throws IOException {
        long expected = Double.doubleToLongBits(Double.parseDouble(text));
        assertEquals(text, expected, Double.doubleToLongBits(JsonReader.newInstance(text).readDouble()));
        assertEquals(text, expected, Double.doubleToLongBits(newJsonSource(text).readDouble()));
    }

    private static JsonSource newJsonSource(String text) {
        return new JsonSource.DefaultJsonSource(JsonReader.newInstance(text));
    }

}
