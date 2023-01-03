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
import protos.test.quickbuf.ForeignEnum;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.external.ImportEnum;
import us.hebi.quickbuf.CompatibilityTest;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.JsonSinkTest;
import us.hebi.quickbuf.JsonSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 25 Feb 2022
 */
public class JacksonSinkTest extends GsonSinkTest {

    public JsonSink newJsonSink() {
        return JacksonSink.newStringWriter();
    }

    public JsonSource newJsonSource(String json) throws IOException {
        return new JacksonSource(new StringReader(json));
    }

}
