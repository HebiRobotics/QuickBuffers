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

package us.hebi.quickbuf.compat;

import org.junit.Test;
import us.hebi.quickbuf.CompatibilityTest;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.JsonSource;
import us.hebi.quickbuf.JsonSourceTest;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author Florian Enner
 * @since 07 Sep 2020
 */
public class JacksonSourceTest extends JsonSourceTest {

    @Test
    public void testBadInputs() {
        testError(CompatibilityTest.JSON_NULL, "Expected { but was VALUE_NULL\n" +
                " at [Source: (StringReader); line: 1, column: 1]");
        testError(CompatibilityTest.JSON_LIST_EMPTY, "Expected { but was START_ARRAY\n" +
                " at [Source: (StringReader); line: 1, column: 1]");
        testError(CompatibilityTest.JSON_REPEATED_BYTES_NULL_VALUE, "Current token (VALUE_NULL) not VALUE_STRING or VALUE_EMBEDDED_OBJECT, can not access as binary\n" +
                " at [Source: (StringReader); line: 1, column: 24]");
        testError(CompatibilityTest.JSON_REPEATED_MSG_NULL_VALUE, "Expected { but was VALUE_NULL\n" +
                " at [Source: (StringReader); line: 1, column: 29]");
        testError(CompatibilityTest.JSON_BAD_BOOLEAN, "Unrecognized token 'fals': was expecting (JSON String, Number (or 'NaN'/'INF'/'+INF'), Array, Object or token 'null', 'true' or 'false')\n" +
                " at [Source: (StringReader); line: 1, column: 22]");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD, "Encountered unknown field: 'unknownField'");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD_NULL, "Encountered unknown field: 'unknownField'");
    }

    @Override
    protected JsonSource newJsonSource(String json) throws IOException {
        return new JacksonSource(json);
    }

    @Override
    protected JsonSource newJsonSource(JsonSink sink) throws IOException {
        return new JacksonSource(new StringReader(sink.toString()));
    }

}
