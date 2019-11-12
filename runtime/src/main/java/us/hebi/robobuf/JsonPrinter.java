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

import us.hebi.robobuf.JsonUtil.*;
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
        BooleanEncoding.writeBoolean(value, output);
        endField();
    }

    @Override
    public void print(String field, int value) {
        startField(field);
        IntegerEncoding.writeInt(value, output);
        endField();
    }

    @Override
    public void print(String field, long value) {
        startField(field);
        IntegerEncoding.writeLong(value, output);
        endField();
    }

    @Override
    public void print(String field, float value) {
        startField(field);
        FloatEncoding.writeFloat(value, output);
        endField();
    }

    @Override
    public void print(String field, double value) {
        startField(field);
        FloatEncoding.writeDouble(value, output);
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
            BooleanEncoding.writeBoolean(value.array[i], output);
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedInt value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            IntegerEncoding.writeInt(value.array[i], output);
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedLong value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            IntegerEncoding.writeLong(value.array[i], output);
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedFloat value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            FloatEncoding.writeFloat(value.array[i], output);
            continueArray();
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedDouble value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            FloatEncoding.writeDouble(value.array[i], output);
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
        writeRawAscii('"');
        writeRawAscii(field);
        writeRawAscii('"');
        writeRawAscii(':');
        space();
    }

    protected void endField() {
        writeRawAscii(',');
    }

    protected void startArray() {
        writeRawAscii('[');
    }

    protected void endArray() {
        removeTrailingComma();
        writeRawAscii(']');
    }

    protected void continueArray() {
        writeRawAscii(',');
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
        writeRawAscii('{');
        indentLevel++;
    }

    protected void endObject() {
        removeTrailingComma();
        indentLevel--;
        newline();
        writeRawAscii('}');
    }

    private void print(StringBuilder value) {
        writeRawAscii('"');
        StringEncoding.writeUtf8(value, output);
        writeRawAscii('"');
    }

    private void print(RepeatedByte value) {
        writeRawAscii('"');
        Base64Encoding.writeBytes(value.array, value.length, output);
        writeRawAscii('"');
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
        writeRawAscii(' ');
    }

    protected void removeTrailingComma() {
        // Called after at least one character, so no need to check bounds
        if (output.array[output.length - 1] == ',') {
            output.length--;
        }
    }

    private final void writeRawAscii(CharSequence value) {
        StringEncoding.writeRawAscii(value, output);
    }

    private final void writeRawAscii(char c) {
        output.add((byte) c);
    }

    protected final RepeatedByte output;
    protected int indentLevel = 0;
    protected int indentCount = 0;

}
