package us.hebi.robobuf;

/**
 * A simple format that is compatible with JSON readers. The output
 * may not be particularly fast.
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
class JsonPrinter implements ProtoPrinter {

    public static JsonPrinter newInstance() {
        return new JsonPrinter();
    }

    private final StringBuilder builder = new StringBuilder(128);
    private final StringBuilder indent = new StringBuilder(16);

    @Override
    public String toString() {
        return builder.toString();
    }

    @Override
    public ProtoPrinter print(ProtoMessage value) {
        startObject();
        value.print(this);
        endObject();
        return this;
    }

    private void incrementIndent() {
        indent.append("  ");
    }

    private void decrementIndent() {
        indent.setLength(indent.length() - 2);
    }

    private JsonPrinter startField(String field) {
        builder.append("\n")
                .append(indent)
                .append("\"").append(field).append("\"")
                .append(": ");
        return this;
    }

    private JsonPrinter endField() {
        builder.append(",");
        return this;
    }

    private void startArray() {
        builder.append("[");
    }

    private void endArray() {
        removeTrailingComma();
        builder.append("]");
    }

    private JsonPrinter startArrayField(String field) {
        startField(field);
        startArray();
        return this;
    }

    private JsonPrinter endArrayField() {
        endArray();
        return endField();
    }

    private void removeTrailingComma() {
        char prevChar = builder.charAt(builder.length() - 1);
        if (prevChar == ',') {
            builder.setLength(builder.length() - 1);
        }
    }

    private JsonPrinter startObject() {
        builder.append("{");
        incrementIndent();
        return this;
    }

    private JsonPrinter endObject() {
        removeTrailingComma();
        decrementIndent();
        builder.append("\n").append(indent).append("}");
        return this;
    }

    private void print(StringBuilder value) {
        builder.append("\"").append(value).append("\"");
    }

    private void print(RepeatedByte value) {
        startArray();
        for (int j = 0; j < value.length; j++) {
            builder.append(value.array[j]).append(',');
        }
        endArray();
    }

    @Override
    public void print(String field, boolean value) {
        startField(field);
        builder.append(value);
        endField();
    }

    @Override
    public void print(String field, int value) {
        startField(field);
        builder.append(value);
        endField();
    }

    @Override
    public void print(String field, long value) {
        startField(field);
        builder.append(value);
        endField();
    }

    @Override
    public void print(String field, float value) {
        startField(field);
        builder.append(value);
        endField();
    }

    @Override
    public void print(String field, double value) {
        startField(field);
        builder.append(value);
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
        startArrayField(field);
        print(value);
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedBoolean value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            builder.append(value.array[i]).append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedInt value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            builder.append(value.array[i]).append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedLong value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            builder.append(value.array[i]).append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedFloat value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            builder.append(value.array[i]).append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedDouble value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            builder.append(value.array[i]).append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedMessage value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            print((ProtoMessage) value.array[i]);
            builder.append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedString value) {
        startArrayField(field);
        for (int i = 0; i < value.length; i++) {
            print(value.array[i]);
            builder.append(',');
        }
        endArrayField();
    }

    @Override
    public void print(String field, RepeatedBytes values) {
        startArrayField(field);
        for (int i = 0; i < values.length; i++) {
            print(values.get(i));
            builder.append(',');
        }
        endArrayField();
    }

}
