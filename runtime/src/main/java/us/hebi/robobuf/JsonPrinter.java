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

import us.hebi.robobuf.JsonUtil.Base64Encoding;
import us.hebi.robobuf.JsonUtil.BooleanEncoding;
import us.hebi.robobuf.JsonUtil.NumberEncoding;
import us.hebi.robobuf.JsonUtil.StringEncoding;
import us.hebi.robobuf.ProtoUtil.Charsets;

import java.util.Arrays;

/**
 * Prints proto messages in a JSON compatible format
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public class JsonPrinter implements TextPrinter {

    public static JsonPrinter newInstance() {
        return newInstance(new RepeatedByte().reserve(128));
    }

    public static JsonPrinter newInstance(RepeatedByte output) {
        return new JsonPrinter().wrap(output);
    }

    public JsonPrinter wrap(RepeatedByte output) {
        if (output == null)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    public JsonPrinter clear() {
        this.output.clear();
        return this;
    }

    public RepeatedByte getBuffer() {
        return output;
    }

    public JsonPrinter setIndentCount(int indentCount) {
        this.indentCount = indentCount;
        return this;
    }

    @Override
    public TextPrinter print(ProtoMessage value) {
        indentLevel++;
        writeChar('{');
        value.print(this);
        removeTrailingComma();
        writeNewline();
        writeChar('}');
        indentLevel--;
        return this;
    }

    @Override
    public void print(byte[] key, boolean value) {
        writeKey(key);
        BooleanEncoding.writeBoolean(value, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, int value) {
        writeKey(key);
        NumberEncoding.writeInt(value, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, long value) {
        writeKey(key);
        NumberEncoding.writeLong(value, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, float value) {
        writeKey(key);
        NumberEncoding.writeFloat(value, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, double value) {
        writeKey(key);
        NumberEncoding.writeDouble(value, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, ProtoMessage value) {
        writeKey(key);
        print(value);
        writeComma();
    }

    @Override
    public void print(byte[] key, StringBuilder value) {
        writeKey(key);
        StringEncoding.writeQuotedUtf8(value, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, RepeatedByte value) {
        writeKey(key);
        Base64Encoding.writeQuotedBase64(value.array, value.length, output);
        writeComma();
    }

    @Override
    public void print(byte[] key, RepeatedBoolean value) {
        startArray(key);
        for (int i = 0; i < value.length; i++) {
            BooleanEncoding.writeBoolean(value.array[i], output);
            writeComma();
        }
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedInt value) {
        startArray(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeInt(value.array[i], output);
            writeComma();
        }
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedLong value) {
        startArray(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeLong(value.array[i], output);
            writeComma();
        }
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedFloat value) {
        startArray(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeFloat(value.array[i], output);
            writeComma();
        }
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedDouble value) {
        startArray(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeDouble(value.array[i], output);
            writeComma();
        }
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedMessage value) {
        startArray(key);
        for (int i = 0; i < value.length; i++) {
            print((ProtoMessage) value.array[i]);
            writeComma();
        }
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedString value) {
        startArray(key);
        indentLevel++;
        for (int i = 0; i < value.length; i++) {
            writeNewline();
            StringEncoding.writeQuotedUtf8(value.array[i], output);
            writeComma();
        }
        indentLevel--;
        writeNewline();
        finishArray();
    }

    @Override
    public void print(byte[] key, RepeatedBytes values) {
        startArray(key);
        indentLevel++;
        for (int i = 0; i < values.length; i++) {
            writeNewline();
            final RepeatedByte value = values.get(i);
            Base64Encoding.writeQuotedBase64(value.array, value.length, output);
            writeComma();
        }
        indentLevel--;
        writeNewline();
        finishArray();
    }

    protected void writeKey(byte[] key) {
        writeNewline();
        final int pos = output.addLength(key.length);
        System.arraycopy(key, 0, output.array, pos, key.length);
        writeSpace();
    }

    protected void startArray(byte[] key) {
        writeKey(key);
        writeChar('[');
    }

    protected void finishArray() {
        removeTrailingComma();
        writeChar(']');
        writeComma();
    }

    private final void writeComma() {
        writeChar(',');
    }

    protected void removeTrailingComma() {
        // Called after at least one character, so no need to check bounds
        final int pos = output.length - 1;
        if (output.array[pos] == ',') {
            output.length = pos;
        }
    }

    private final void writeNewline() {
        if (indentCount <= 0)
            return;
        final int numSpaces = indentLevel * indentCount;
        int pos = output.addLength(numSpaces + 1);
        output.array[pos++] = '\n';
        Arrays.fill(output.array, pos, output.length, (byte) ' ');
    }

    private final void writeSpace() {
        if (indentCount <= 0)
            return;
        writeChar(' ');
    }

    private final void writeChar(char c) {
        final int pos = output.addLength(1);
        output.array[pos] = (byte) c;
    }

    @Override
    public String toString() {
        return new String(output.array, 0, output.length, Charsets.UTF_8);
    }

    protected JsonPrinter() {
    }

    protected RepeatedByte output;
    protected int indentLevel = 0;
    protected int indentCount = 0;

}
