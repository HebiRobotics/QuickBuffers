package us.hebi.robobuf;

import org.junit.Test;
import us.hebi.robobuf.robo.RepeatedPackables;
import us.hebi.robobuf.robo.TestAllTypes;

import java.io.IOException;

/**
 * @author Florian Enner
 * @since 15 Aug 2019
 */
public class FailTests {

    @Test(expected = IllegalStateException.class)
    public void testMissingRequiredField() {
        new TestAllTypes().toByteArray();
    }

    // --------------------------------------------------------------------------------------

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceVarint32() throws IOException {
        // id is at the very end, so don't set it for other types
        writeToTruncated(new TestAllTypes().setId(Integer.MIN_VALUE));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceVarint64() throws IOException {
        writeToTruncated(new TestAllTypes().setDefaultInt64(Long.MIN_VALUE));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceFixed32() throws IOException {
        writeToTruncated(new TestAllTypes().setDefaultFixed32(Integer.MAX_VALUE));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceFixed64() throws IOException {
        writeToTruncated(new TestAllTypes().setDefaultFixed64(Long.MAX_VALUE));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceString() throws IOException {
        writeToTruncated(new TestAllTypes().setOptionalString("this should fail"));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceBytes() throws IOException {
        writeToTruncated(new TestAllTypes().addAllDefaultBytes(new byte[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedBoolean() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllBools(new boolean[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedDouble() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllDoubles(new double[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedFloat() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllFloats(new float[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedVarint32() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllInt32S(new int[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedFixed32() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllFixed32S(new int[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedVarint64() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllInt64S(new long[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpacePackedFixed64() throws IOException {
        writeToTruncated(new RepeatedPackables.Packed().addAllFixed64S(new long[213]));
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceMessage() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalNestedMessage().setBb(1);
        writeToTruncated(msg);
    }

    @Test(expected = ProtoOutputBuffer.OutOfSpaceException.class)
    public void testOutOfSpaceGroup() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalGroup().setA(2);
        writeToTruncated(msg);
    }

    private void writeToTruncated(MessageNano msg) throws IOException {
        byte[] buffer = new byte[msg.getSerializedSize() - 1];
        ProtoOutputBuffer output = ProtoOutputBuffer.newInstance(buffer);
        msg.writeTo(output);
    }

    // --------------------------------------------------------------------------------------

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedVarint32() throws IOException {
        readFromTruncated(new TestAllTypes().setId(Integer.MIN_VALUE));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedVarint64() throws IOException {
        readFromTruncated(new TestAllTypes().setDefaultInt64(Long.MIN_VALUE));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedFixed32() throws IOException {
        readFromTruncated(new TestAllTypes().setDefaultFixed32(Integer.MAX_VALUE));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedFixed64() throws IOException {
        readFromTruncated(new TestAllTypes().setDefaultFixed64(Long.MAX_VALUE));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedString() throws IOException {
        readFromTruncated(new TestAllTypes().setOptionalString("this should fail"));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedBytes() throws IOException {
        readFromTruncated(new TestAllTypes().addAllDefaultBytes(new byte[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedBoolean() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllBools(new boolean[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedDouble() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllDoubles(new double[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedFloat() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllFloats(new float[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedVarint32() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllInt32S(new int[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedFixed32() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllFixed32S(new int[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedVarint64() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllInt64S(new long[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedPackedFixed64() throws IOException {
        readFromTruncated(new RepeatedPackables.Packed().addAllFixed64S(new long[213]));
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedMessage() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalNestedMessage().setBb(1);
        readFromTruncated(msg);
    }

    @Test(expected = InvalidProtocolBufferNanoException.class)
    public void testTruncatedGroup() throws IOException {
        TestAllTypes msg = new TestAllTypes();
        msg.getMutableOptionalGroup().setA(2);
        readFromTruncated(msg);
    }

    private void readFromTruncated(MessageNano msg) throws IOException {
        int length = msg.computeSerializedSize() - 1; // without id field
        if (msg instanceof TestAllTypes) {
            // Required field needs to be set, but it's at the end
            TestAllTypes allTypes = ((TestAllTypes) msg);
            if (!allTypes.hasId()) {
                allTypes.setId(0);
            }
        }

        byte[] data = msg.toByteArray();
        ProtoInputBuffer input = ProtoInputBuffer.newInstance(data, 0, length);
        msg.clear().mergeFrom(input);
    }

    private static final int RequiredIdLength = new TestAllTypes().setId(0).getSerializedSize();

    // --------------------------------------------------------------------------------------

}
