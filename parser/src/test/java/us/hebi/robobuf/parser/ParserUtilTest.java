package us.hebi.robobuf.parser;

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