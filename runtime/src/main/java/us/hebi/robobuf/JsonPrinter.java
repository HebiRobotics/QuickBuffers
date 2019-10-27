package us.hebi.robobuf;

/**
 * Prints proto messages in a JSON compatible format
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public class JsonPrinter implements ProtoPrinter {

    public static JsonPrinter newInstance() {
        return new JsonPrinter();
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

    protected void incrementIndent() {
        indent.append("  ");
    }

    protected void decrementIndent() {
        indent.setLength(indent.length() - 2);
    }

    protected void startField(String field) {
        builder.append("\n")
                .append(indent)
                .append("\"").append(field).append("\"")
                .append(": ");
    }

    protected void endField() {
        builder.append(",");
    }

    protected void startArray() {
        builder.append("[");
    }

    protected void endArray() {
        removeTrailingComma();
        builder.append("]");
    }

    protected void startArrayField(String field) {
        startField(field);
        startArray();
    }

    protected void endArrayField() {
        endArray();
        endField();
    }

    protected void removeTrailingComma() {
        char prevChar = builder.charAt(builder.length() - 1);
        if (prevChar == ',') {
            builder.setLength(builder.length() - 1);
        }
    }

    protected void startObject() {
        builder.append("{");
        incrementIndent();
    }

    protected void endObject() {
        removeTrailingComma();
        decrementIndent();
        builder.append("\n").append(indent).append("}");
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
    public String toString() {
        return builder.toString();
    }

    protected JsonPrinter() {
    }

    protected final StringBuilder builder = new StringBuilder(128);
    protected final StringBuilder indent = new StringBuilder(16);

}
