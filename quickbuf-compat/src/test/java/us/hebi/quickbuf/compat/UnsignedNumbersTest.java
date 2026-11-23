/*-
 * #%L
 * quickbuf-compat
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

package us.hebi.quickbuf.compat;

import org.junit.Test;
import us.hebi.quickbuf.InvalidJsonException;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 23 Nov 2026
 */
public class UnsignedNumbersTest {

    @Test
    public void testValidValues() throws InvalidJsonException {
        assertEquals(0, UnsignedNumbers.parseUInt64("0"));
        assertEquals(0, UnsignedNumbers.parseUInt64("-0.0"));
        assertEquals(0, UnsignedNumbers.parseUInt64("0e100000000"));
        assertEquals(1, UnsignedNumbers.parseUInt64("1.0"));
        assertEquals(Long.parseUnsignedLong("10000000000000000000"), UnsignedNumbers.parseUInt64("1e19"));
        assertEquals(-1, UnsignedNumbers.parseUInt64("18446744073709551615"));
        assertEquals(Long.MIN_VALUE, UnsignedNumbers.parseUInt64("-9223372036854775808"));
    }

    @Test
    public void testInvalidValues() {
        assertInvalid("abc");
        assertInvalid("1.5");
        assertInvalid("0.5");
        assertInvalid("18446744073709551616");
        assertInvalid("-9223372036854775809");
        assertInvalid("1e20");
    }

    @Test(timeout = 1000)
    public void testHugeExponents() {
        assertInvalid("1e10000000");
        assertInvalid("1e100000000");
        assertInvalid("1e-100000000");
        assertInvalid("-1e100000000");
    }

    private static void assertInvalid(String value) {
        try {
            UnsignedNumbers.parseUInt64(value);
            fail("Expected an exception for '" + value + "'");
        } catch (InvalidJsonException expected) {
            assertEquals("Invalid uint64 value: '" + value + "'", expected.getMessage());
        }
    }

}
