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

import us.hebi.quickbuf.InvalidJsonException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Unsigned 64 bit numbers for JSON libraries that only support signed longs
 *
 * @author Florian Enner
 * @since 23 Nov 2026
 */
final class UnsignedNumbers {

    /**
     * Parses a uint64 value, including negative values written by older versions
     */
    static long parseUInt64(String value) throws InvalidJsonException {
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.signum() == 0) {
                return 0;
            }
            long integerDigits = (long) decimal.precision() - decimal.scale();
            if (integerDigits > 0 && integerDigits <= 20) {
                BigInteger number = decimal.toBigIntegerExact();
                if (number.bitLength() <= (number.signum() < 0 ? 63 : 64)) {
                    return number.longValue();
                }
            }
        } catch (NumberFormatException | ArithmeticException invalid) {
            // fall through
        }
        throw new InvalidJsonException("Invalid uint64 value: '" + value + "'");
    }

    private UnsignedNumbers() {
    }

}
