package us.hebi.robobuf;

/**
 * Provides a text output for messages
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public interface ProtoPrinter {

    // ======== Root Message ========

    public ProtoPrinter print(ProtoMessage value);

    // ======== Optional / Required ========

    public void print(String field, boolean value);

    public void print(String field, int value);

    public void print(String field, long value);

    public void print(String field, float value);

    public void print(String field, double value);

    public void print(String field, ProtoMessage value);

    public void print(String field, StringBuilder value);

    public void print(String field, RepeatedByte value);

    // ======== Repeated ========

    public void print(String field, RepeatedBoolean value);

    public void print(String field, RepeatedInt value);

    public void print(String field, RepeatedLong value);

    public void print(String field, RepeatedFloat value);

    public void print(String field, RepeatedDouble value);

    public void print(String field, RepeatedMessage value);

    public void print(String field, RepeatedString value);

    public void print(String field, RepeatedBytes value);

}
