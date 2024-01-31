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

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 07 Sep 2020
 */
public class GsonSourceTest extends JsonSourceTest {

    @Test
    public void testBadInputs() {
        testError(CompatibilityTest.JSON_NULL, "Expected BEGIN_OBJECT but was NULL at line 1 column 5 path $");
        testError(CompatibilityTest.JSON_LIST_EMPTY, "Expected BEGIN_OBJECT but was BEGIN_ARRAY at line 1 column 2 path $");
        testError(CompatibilityTest.JSON_REPEATED_BYTES_NULL_VALUE, "Expected a string but was NULL at line 1 column 24 path $.repeatedBytes[0]");
        testError(CompatibilityTest.JSON_REPEATED_MSG_NULL_VALUE, "Expected BEGIN_OBJECT but was NULL at line 1 column 33 path $.repeatedForeignMessage[0]");
        testError(CompatibilityTest.JSON_BAD_BOOLEAN, "Expected a boolean but was STRING at line 1 column 18 path $.optionalBool");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD, "Encountered unknown field: 'unknownField'");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD_NULL, "Encountered unknown field: 'unknownField'");
    }

    @Override
    protected JsonSource newJsonSource(String json) {
        return new GsonSource(json);
    }

    @Override
    protected JsonSource newJsonSource(JsonSink sink) {
        return new GsonSource(new StringReader(sink.toString()));
    }

    @Override
    protected void testError(String input, String error) {
        try {
            parseJson(input);
            fail("expected error: " + error);
        } catch (IOException ioe) {
            assertEquals(error, ioe.getMessage());
        } catch (IllegalStateException stateEx) {
            // TODO: wrap IllegalStateException?
            assertEquals(error, stateEx.getMessage());
        }
    }

}
