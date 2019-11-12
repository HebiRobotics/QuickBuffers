/*-
 * #%L
 * robobuf-runtime
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

package us.hebi.robobuf;

import us.hebi.robobuf.ProtoUtil.Charsets;

/**
 * Prints proto messages in a JSON compatible format
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public class JsonPrinter implements ProtoPrinter {

    public static JsonPrinter newInstance() {
        return wrap(new RepeatedByte().reserve(128));
    }

    public static JsonPrinter wrap(RepeatedByte output) {
        return new JsonPrinter(output);
    }

    public JsonPrinter setIndentCount(int indentCount) {
        this.indentCount = indentCount;
        return this;
    }

    @Override
    public ProtoPrinter print(ProtoMessage value) {
        startObject();
        value.print(this);
        endObject();
        return this;
    }

    @Override
    public void print(String field, boolean value) {
        startField(field);
        if (value) {
            writeUnescapedAscii("true");
        } else {
            writeUnescapedAscii("false");
        }
        endField();
    }

    @Override
    public void print(String field, int value) {
        startField(field);
        writeUnescapedAscii(String.valueOf(value));
        endField();
    }

    @Override
    public void print(String field, long value) {
        startField(field);
        writeUnescapedAscii(String.valueOf(value));
        endField();
    }

    @Override
    public void print(String field, float value) {
        startField(field);
        writeUnescapedAscii(String.valueOf(value));
        endField();
    }

    @Override
    public void print(String field, double value) {
        startField(field);
        writeUnescapedAscii(String.valueOf(value));
        endField();
    }

    @Override
    public void print(String field, ProtoMessage value) {
        startField(field);
        startObject();
        value.print(this);
        endObject();
        endField();
    }

    @Override
    public void print(String field, StringBuilder value) {
        startField(field);
        print(value);
        endField();
    }

    @Override
    public void print(String field, RepeatedByte value) {
        startField(field);
        print(value);
        endField();
    }

    @Override
    public void print(String field, RepeatedBoolean value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            writeUnescapedAscii(String.valueOf(value.array[i]));
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedInt value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            writeUnescapedAscii(String.valueOf(value.array[i]));
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedLong value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            writeUnescapedAscii(String.valueOf(value.array[i]));
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedFloat value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            writeUnescapedAscii(String.valueOf(value.array[i]));
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedDouble value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            writeUnescapedAscii(String.valueOf(value.array[i]));
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedMessage value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            print((ProtoMessage) value.array[i]);
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedString value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            print(value.array[i]);
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedBytes values) {
        startArrayField(field);
        for (int i = 0; i < values.length; i++) {
            print(values.get(i));
            continueArray();
        }
        endArrayField();
    }

    protected void startField(String field) {
        newline();
        writeUnescapedAscii('"');
        writeUnescapedAscii(field);
        writeUnescapedAscii('"');
        writeUnescapedAscii(':');
        space();
    }

    protected void endField() {
        writeUnescapedAscii(',');
    }

    protected void startArray() {
        writeUnescapedAscii('[');
    }

    protected void endArray() {
        removeTrailingComma();
        writeUnescapedAscii(']');
    }

    protected void continueArray() {
        writeUnescapedAscii(',');
    }

    protected void startArrayField(String field) {
        startField(field);
        startArray();
    }

    protected void endArrayField() {
        endArray();
        endField();
    }

    protected void startObject() {
        writeUnescapedAscii('{');
        indentLevel++;
    }

    protected void endObject() {
        removeTrailingComma();
        indentLevel--;
        newline();
        writeUnescapedAscii('}');
    }

    private void print(StringBuilder value) {
        writeUnescapedAscii('"');
        writeEscapedUtf8(value);
        writeUnescapedAscii('"');
    }

    private void print(RepeatedByte value) {
        writeUnescapedAscii('"');
        writeBase64(value.array, value.length);
        writeUnescapedAscii('"');
    }

    @Override
    public String toString() {
        return new String(output.array, 0, output.length, Charsets.UTF_8);
    }

    protected JsonPrinter(RepeatedByte output) {
        if (output == null)
            throw new NullPointerException();
        this.output = output;
    }

    private final void newline() {
        if (indentCount <= 0)
            return;
        int numSpaces = indentLevel * indentCount;
        output.reserve(1 + numSpaces);
        output.array[output.length++] = '\n';
        for (int i = 0; i < numSpaces; i++) {
            output.array[output.length++] = ' ';
        }
    }

    private final void space() {
        if (indentCount <= 0)
            return;
        writeUnescapedAscii(' ');
    }

    protected void removeTrailingComma() {
        // Called after at least one character, so no need to check bounds
        if (output.array[output.length - 1] == ',') {
            output.length--;
        }
    }

    private final void writeBase64(final byte[] bytes, final int length) {
        final int encodedLength = ((length - 1) / 3 + 1) << 2;
        output.reserve(encodedLength);

        // Encode 24-bit blocks
        int i;
        final int blockableLength = (length / 3) * 3; // Length of even 24-bits.
        for (i = 0; i < blockableLength; ) {
            // Copy next three bytes into lower 24 bits of int
            final int bits = (bytes[i++] & 0xff) << 16 | (bytes[i++] & 0xff) << 8 | (bytes[i++] & 0xff);

            // Encode the 24 bits into four characters
            output.array[output.length++] = BASE64[(bits >>> 18) & 0x3f];
            output.array[output.length++] = BASE64[(bits >>> 12) & 0x3f];
            output.array[output.length++] = BASE64[(bits >>> 6) & 0x3f];
            output.array[output.length++] = BASE64[bits & 0x3f];
        }

        // Pad and encode last bits if source isn't even 24 bits.
        int remaining = length - blockableLength; // 0 - 2.
        if (remaining > 0) {
            // Prepare the int
            int bits = ((bytes[i++] & 0xff) << 10) | (remaining == 2 ? ((bytes[i] & 0xff) << 2) : 0);

            // Set last four bytes
            output.array[output.length++] = BASE64[bits >> 12];
            output.array[output.length++] = BASE64[(bits >>> 6) & 0x3f];
            output.array[output.length++] = remaining == 2 ? BASE64[bits & 0x3f] : (byte) '=';
            output.array[output.length++] = '=';
        }

    }

    private static final byte[] BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(Charsets.ISO_8859_1);

    private final void writeUnescapedAscii(CharSequence value) {
        final int length = value.length();
        output.reserve(length);
        for (int i = 0; i < length; i++) {
            output.array[output.length++] = (byte) value.charAt(i);
        }
    }

    private final void writeEscapedUtf8(CharSequence sequence) {
        final int numChars = sequence.length();
        if (numChars == 0) return;
        int i = 0;

        // Fast-path: no utf8 and escape support
        output.reserve(numChars);
        for (; i < numChars; i++) {
            final char c = sequence.charAt(i);
            if (c < 128 && CAN_DIRECT_WRITE[c]) {
                output.array[output.length++] = (byte) c;
            } else {
                break;
            }
        }

        // Slow-path: utf8 and/or escaping
        for (; i < numChars; i++) {
            final char c = sequence.charAt(i);

            if (c < 0x80) { // ascii
                if (CAN_DIRECT_WRITE[c]) {
                    writeUnescapedAscii(c);
                } else {
                    writeEscapedAscii(c);
                }

            } else if (c < 0x800) { // 11 bits, two UTF-8 bytes
                write(
                        (byte) ((0xF << 6) | (c >>> 6)),
                        (byte) (0x80 | (0x3F & c))
                );
            } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c)) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                write(
                        (byte) ((0xF << 5) | (c >>> 12)),
                        (byte) (0x80 | (0x3F & (c >>> 6))),
                        (byte) (0x80 | (0x3F & c))
                );
            } else {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                final char low;
                if (i + 1 == numChars || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                    throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                }
                int codePoint = Character.toCodePoint(c, low);
                write(
                        (byte) ((0xF << 4) | (codePoint >>> 18)),
                        (byte) (0x80 | (0x3F & (codePoint >>> 12))),
                        (byte) (0x80 | (0x3F & (codePoint >>> 6))),
                        (byte) (0x80 | (0x3F & codePoint))

                );
            }
        }

    }

    private final void writeUnescapedAscii(char c) {
        write((byte) c);
    }

    private final void writeEscapedAscii(char c) {
        switch (c) {
            case '"':
                write((byte) '\\', (byte) '"');
                break;
            case '\\':
                write((byte) '\\', (byte) '\\');
                break;
            case '\b':
                write((byte) '\\', (byte) 'b');
                break;
            case '\f':
                write((byte) '\\', (byte) 'f');
                break;
            case '\n':
                write((byte) '\\', (byte) 'n');
                break;
            case '\r':
                write((byte) '\\', (byte) 'r');
                break;
            case '\t':
                write((byte) '\\', (byte) 't');
                break;
            default:
                writeAsSlashU(c);
        }
    }

    private void writeAsSlashU(int c) {
        output.reserve(6);
        output.array[output.length++] = '\\';
        output.array[output.length++] = 'u';
        output.array[output.length++] = ITOA[c >> 12 & 0xf];
        output.array[output.length++] = ITOA[c >> 8 & 0xf];
        output.array[output.length++] = ITOA[c >> 4 & 0xf];
        output.array[output.length++] = ITOA[c & 0xf];
    }

    private static final byte[] ITOA = new byte[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};

    private static final boolean[] CAN_DIRECT_WRITE = new boolean[128];

    static {
        for (int i = 0; i < CAN_DIRECT_WRITE.length; i++) {
            if (i > 31 && i < 126 && i != '"' && i != '\\') {
                CAN_DIRECT_WRITE[i] = true;
            }
        }
    }


    private final void write(byte b0) {
        output.reserve(1);
        output.array[output.length++] = b0;
    }

    private final void write(byte b0, byte b1) {
        output.reserve(2);
        output.array[output.length++] = b0;
        output.array[output.length++] = b1;
    }

    private final void write(byte b0, byte b1, byte b2) {
        output.reserve(3);
        output.array[output.length++] = b0;
        output.array[output.length++] = b1;
        output.array[output.length++] = b2;
    }

    private final void write(byte b0, byte b1, byte b2, byte b3) {
        output.reserve(4);
        output.array[output.length++] = b0;
        output.array[output.length++] = b1;
        output.array[output.length++] = b2;
        output.array[output.length++] = b3;
    }

    protected final RepeatedByte output;
    protected int indentLevel = 0;
    protected int indentCount = 0;

}
