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

package us.hebi.quickbuf;

import us.hebi.quickbuf.JsonSource.ByteSource;

import java.io.IOException;

import static us.hebi.quickbuf.IntChar.*;

/**
 * @author Florian Enner
 * @since 16 Mär 2022
 */
public class JsonSkips {/*

    public static int nextToken(ByteSource source) throws IOException {
        int ch = source.read();
        while (isWhitespace(ch)) {
            ch = source.read();
        }
        return ch;
    }

    public static void skip(ByteSource source) throws IOException {
        int c = nextToken(source);
        switch (c) {
            case '"':
                skipString(iter);
                return;
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                skipUntilBreak(iter);
                return;
            case 't':
            case 'n':
                skipFixedBytes(iter, 3); // true or null
                return;
            case 'f':
                skipFixedBytes(iter, 4); // false
                return;
            case '[':
                skipArray(iter);
                return;
            case '{':
                skipObject(iter);
                return;
            default:
                throw iter.reportError("IterImplSkip", "do not know how to skip: " + c);
        }
    }

*/}
