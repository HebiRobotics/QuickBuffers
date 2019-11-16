/*-
 * #%L
 * quickbuf-parser
 * %%
 * Copyright (C) 2019 HEBI Robotics
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
