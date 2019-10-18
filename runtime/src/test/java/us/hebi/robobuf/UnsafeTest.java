package us.hebi.robobuf;

import org.junit.Before;
import org.junit.Test;
import sun.nio.ch.DirectBuffer;
import protos.test.robo.RepeatedPackables;
import protos.test.robo.TestAllTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 20 Aug 2019
 */
public class UnsafeTest {

    TestAllTypes message;
    byte[] array;
    ByteBuffer directBuffer;
    long directAddress;

    @Before
    public void setupData() throws IOException {
        message = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        array = message.toByteArray();
        directBuffer = ByteBuffer.allocateDirect(array.length);
        directBuffer.put(array);
        directBuffer.rewind();
        directAddress = getDirectAddress(directBuffer);
    }

    @Test
    public void testUnsafeArray() throws IOException {
        // Write
        int offset = 11;
        byte[] target = new byte[array.length + offset]; // make sure offsets are handled correctly
        ProtoSink sink = ProtoSink.createUnsafe().setOutput(target, offset, target.length - offset);
        message.writeTo(sink);
        sink.checkNoSpaceLeft();
        byte[] actual = Arrays.copyOfRange(target, offset, target.length);
        assertArrayEquals(array, actual);

        // Read
        ProtoSource source = ProtoSource.createUnsafe().setInput(target, offset, target.length - offset);
        assertEquals(message, new TestAllTypes().mergeFrom(source));
    }

    @Test
    public void testUnsafeDirectMemory() throws IOException {
        // Write
        ProtoSink sink = ProtoSink.createUnsafe().setOutput(null, directAddress, array.length);
        message.writeTo(sink);
        byte[] actual = new byte[sink.position()];
        sink.checkNoSpaceLeft();
        directBuffer.get(actual);
        assertArrayEquals(array, actual);

        // Read
        ProtoSource source = ProtoSource.createUnsafe().setInput(null, directAddress, array.length);
        assertEquals(message, new TestAllTypes().mergeFrom(source));
    }

    @Test
    public void testRepeatedPacked() throws IOException {
        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesPacked());
        int size = msg.getSerializedSize();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        ProtoSink sink = ProtoSink.createUnsafe().setOutput(null, getDirectAddress(buffer), size);
        msg.writeTo(sink);
        sink.checkNoSpaceLeft();

        ProtoSource source = ProtoSource.createUnsafe().setInput(null, getDirectAddress(buffer), size);
        assertEquals(msg, new RepeatedPackables.Packed().mergeFrom(source));
    }

    @Test
    public void testRepeatedNonPacked() throws IOException {
        RepeatedPackables.NonPacked msg = RepeatedPackables.NonPacked.parseFrom(CompatibilityTest.repeatedPackablesNonPacked());
        int size = msg.getSerializedSize();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        ProtoSink sink = ProtoSink.createUnsafe().setOutput(null, getDirectAddress(buffer), size);
        msg.writeTo(sink);
        sink.checkNoSpaceLeft();

        ProtoSource source = ProtoSource.createUnsafe().setInput(null, getDirectAddress(buffer), size);
        assertEquals(msg, new RepeatedPackables.NonPacked().mergeFrom(source));
    }

    private static long getDirectAddress(ByteBuffer buffer) {
        return ((DirectBuffer) buffer).address();
    }

}
