/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
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

import org.junit.Before;
import org.junit.Test;
import sun.nio.ch.DirectBuffer;
import protos.test.quickbuf.RepeatedPackables;
import protos.test.quickbuf.TestAllTypes;

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
        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(target, offset, target.length - offset);
        message.writeTo(sink);
        sink.checkNoSpaceLeft();
        byte[] actual = Arrays.copyOfRange(target, offset, target.length);
        assertArrayEquals(array, actual);

        // Read
        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(target, offset, target.length - offset);
        assertEquals(message, TestAllTypes.newInstance().mergeFrom(source));
    }

    @Test
    public void testUnsafeDirectMemory() throws IOException {
        // Write
        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(null, directAddress, array.length);
        message.writeTo(sink);
        byte[] actual = new byte[sink.position()];
        sink.checkNoSpaceLeft();
        directBuffer.get(actual);
        assertArrayEquals(array, actual);

        // Read
        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(null, directAddress, array.length);
        assertEquals(message, TestAllTypes.newInstance().mergeFrom(source));
    }

    @Test
    public void testRepeatedPacked() throws IOException {
        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesPacked());
        int size = msg.getSerializedSize();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(null, getDirectAddress(buffer), size);
        msg.writeTo(sink);
        sink.checkNoSpaceLeft();

        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(null, getDirectAddress(buffer), size);
        assertEquals(msg, RepeatedPackables.Packed.newInstance().mergeFrom(source));
    }

    @Test
    public void testRepeatedNonPacked() throws IOException {
        RepeatedPackables.NonPacked msg = RepeatedPackables.NonPacked.parseFrom(CompatibilityTest.repeatedPackablesNonPacked());
        int size = msg.getSerializedSize();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(null, getDirectAddress(buffer), size);
        msg.writeTo(sink);
        sink.checkNoSpaceLeft();

        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(null, getDirectAddress(buffer), size);
        assertEquals(msg, RepeatedPackables.NonPacked.newInstance().mergeFrom(source));
    }

    private static long getDirectAddress(ByteBuffer buffer) {
        return ((DirectBuffer) buffer).address();
    }

}
