package us.hebi.robobuf;

import org.junit.Test;
import us.hebi.robobuf.robo.RepeatedPackables;
import us.hebi.robobuf.robo.TestAllTypes;
import us.hebi.robobuf.robo.UnittestRequired;

import java.io.IOException;

/**
 * @author Florian Enner
 * @since 15 Aug 2019
 */
public class ProtoFailTests {

    @Test(expected = IllegalStateException.class)
    public void testMissingRequiredField() {
        new UnittestRequired.SimpleMessage().toByteArray();
    }

    // --------------------------------------------------------------------------------------

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceVarint32() throws IOException {
        // id is at the very end, so don't set it for other types
        writeToTruncated(new TestAllTypes().setId(Integer.MIN_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceVarint64() throws IOException {
        writeToTruncated(new TestAllTypes().setDefaultInt64(Long.MIN_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceFixed32() throws IOException {
        writeToTruncated(new TestAllTypes().setDefaultFixed32(Integer.MAX_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceFixed64() throws IOException {
        writeToTruncated(new TestAllTypes().setDefaultFixed64(Long.MAX_VALUE));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceString() throws IOException {
        writeToTruncated(new TestAllTypes().setOptionalString("this should fail"));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceBytes() throws IOException {
        writeToTruncated(new TestAllTypes().addAllDefaultBytes(new byte[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedBoolean() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllBools(new boolean[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedDouble() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllDoubles(new double[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedFloat() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllFloats(new float[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedVarint32() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllInt32S(new int[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedFixed32() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllFixed32S(new int[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedVarint64() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllInt64S(new long[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpacePackedFixed64() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllFixed64S(new long[213]));
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceMessage() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalNestedMessage().setBb(1);
        writeToTruncated(msg);
    }

    @Test(expected = ProtoSink.OutOfSpaceException.class)
    public void testOutOfSpaceGroup() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalGroup().setA(2);
        writeToTruncated(msg);
    }

    private void writeToTruncated(ProtoMessage msg) throws IOException {
        byte[] buffer = new byte[msg.getSerializedSize() - 1];
        // use unsafe to make sure that we aren't relying on normal array checks
        ProtoSink output = ProtoSink.createUnsafe().setOutput(buffer);
        msg.writeTo(output);
    }

    // --------------------------------------------------------------------------------------

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedVarint32() throws IOException {
        readFromTruncated(new TestAllTypes().setId(Integer.MIN_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedVarint64() throws IOException {
        readFromTruncated(new TestAllTypes().setDefaultInt64(Long.MIN_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedFixed32() throws IOException {
        readFromTruncated(new TestAllTypes().setDefaultFixed32(Integer.MAX_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedFixed64() throws IOException {
        readFromTruncated(new TestAllTypes().setDefaultFixed64(Long.MAX_VALUE));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedString() throws IOException {
        readFromTruncated(new TestAllTypes().setOptionalString("this should fail"));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedBytes() throws IOException {
        readFromTruncated(new TestAllTypes().addAllDefaultBytes(new byte[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedBoolean() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllBools(new boolean[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedDouble() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllDoubles(new double[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedFloat() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllFloats(new float[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedVarint32() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllInt32S(new int[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedFixed32() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllFixed32S(new int[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedVarint64() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllInt64S(new long[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedPackedFixed64() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllFixed64S(new long[213]));
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedMessage() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalNestedMessage().setBb(1);
        readFromTruncated(msg);
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testTruncatedGroup() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalGroup().setA(2);
        readFromTruncated(msg);
    }

    private void readFromTruncated(ProtoMessage msg) throws IOException {
        byte[] data = msg.toByteArray();
        ProtoSource input = ProtoSource.createUnsafe().setInput(data, 0, data.length - 1);
        msg.clear().mergeFrom(input);
    }

    // --------------------------------------------------------------------------------------

}
