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

/**
 * @author Florian Enner
 * @since 14 Mär 2022
 */
public class IntChar {

    public final static int INT_EOF = -1;
    public final static int INT_TAB = '\t';
    public final static int INT_LF = '\n';
    public final static int INT_CR = '\r';
    public final static int INT_SPACE = 0x0020;

    // Markup
    public final static int INT_LBRACKET = '[';
    public final static int INT_RBRACKET = ']';
    public final static int INT_LCURLY = '{';
    public final static int INT_RCURLY = '}';
    public final static int INT_QUOTE = '"';
    public final static int INT_APOS = '\'';
    public final static int INT_BACKSLASH = '\\';
    public final static int INT_SLASH = '/';
    public final static int INT_ASTERISK = '*';
    public final static int INT_COLON = ':';
    public final static int INT_COMMA = ',';
    public final static int INT_HASH = '#';

    // Number chars
    public final static int INT_0 = '0';
    public final static int INT_9 = '9';
    public final static int INT_MINUS = '-';
    public final static int INT_PLUS = '+';

    public final static int INT_PERIOD = '.';
    public final static int INT_e = 'e';
    public final static int INT_E = 'E';

    public final static int INT_t = 't';
    public final static int INT_r = 'r';
    public final static int INT_u = 'u';
    public final static int INT_f = 'f';
    public final static int INT_a = 'a';
    public final static int INT_l = 'l';
    public final static int INT_s = 's';
    public final static int INT_n = 'n';


    public static boolean isDigit(int ch) {
        return ch >= INT_0 && ch <= INT_9;
    }

    public static boolean isWhitespace(int ch) {
        return ch == INT_SPACE
                || ch == INT_LF
                || ch == INT_CR
                || ch == INT_TAB;
    }

    public static boolean isBreak(int ch) {
        return breaks[ch];
    }

    private static final boolean[] breaks = new boolean[127];

    static {
        breaks[' '] = true;
        breaks['\t'] = true;
        breaks['\n'] = true;
        breaks['\r'] = true;
        breaks[','] = true;
        breaks['}'] = true;
        breaks[']'] = true;
    }

}
