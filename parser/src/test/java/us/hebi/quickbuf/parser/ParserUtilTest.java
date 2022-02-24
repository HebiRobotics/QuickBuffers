/*-
 * #%L
 * quickbuf-parser
 * %%
 * Copyright (C) 2019 HEBI Robotics
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

package us.hebi.quickbuf.parser;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
public class ParserUtilTest {

    @Test
    public void parseEmptyParameters() {
        assertTrue(ParserUtil.parseGeneratorParameters("").isEmpty());
    }

    @Test
    public void parseSingleParameter() {
        assertEquals("value",ParserUtil.parseGeneratorParameters("key=value").get("key"));
        assertEquals("",ParserUtil.parseGeneratorParameters("key").get("key"));
    }

    @Test
    public void parseMultiParameter() {
        Map<String,String> params = ParserUtil.parseGeneratorParameters("key1=value1,key2=value2,key3");
        assertEquals("value1",params.get("key1"));
        assertEquals("value2",params.get("key2"));
        assertEquals("",params.get("key3"));
    }

}
