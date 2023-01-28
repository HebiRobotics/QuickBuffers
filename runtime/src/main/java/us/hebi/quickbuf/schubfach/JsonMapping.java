/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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
package us.hebi.quickbuf.schubfach;

import us.hebi.quickbuf.RepeatedByte;

import java.nio.charset.Charset;

/**
 * Utilities for formatting numbers according to the proto3 json mapping
 * <a href="https://developers.google.com/protocol-buffers/docs/proto3#json">proto3#json</a>
 *
 * JSON value will be a number or one of the special string values "NaN", "Infinity", and
 * "-Infinity". Either numbers or strings are accepted. Exponent notation is also accepted.
 * -0 is considered equivalent to 0.
 *
 * @author Florian Enner
 * @since 28 Jan 2023
 */
class JsonMapping {

    static RepeatedByte writeNumberTo(final byte[] bytes, int index, RepeatedByte output) {
        // Protobuf spec does not call for a trailing zero
        return output.addAll(bytes, 0, getLengthWithoutTrailingZero(bytes, index));
    }

    static int getLengthWithoutTrailingZero(byte[] bytes, int lastIndex) {
        // The Protobuf JSON mapping does not call for a trailing zero
        if (bytes[lastIndex] == '0' && bytes[lastIndex - 1] == '.') {
            return lastIndex - 1;
        } else {
            return lastIndex + 1;
        }
    }

    private static final Charset ASCII = Charset.forName("US-ASCII");
    static final byte[] PLUS_ZERO = "0".getBytes(ASCII);
    static final byte[] MINUS_ZERO = PLUS_ZERO;
    static final byte[] PLUS_INF = "\"Infinity\"".getBytes(ASCII);
    static final byte[] MINUS_INF = "\"-Infinity\"".getBytes(ASCII);
    static final byte[] NAN = "\"NaN\"".getBytes(ASCII);

}
