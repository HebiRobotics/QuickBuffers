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
import protos.test.quickbuf.RepeatedPackables;
import protos.test.quickbuf.TestAllTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.*;
import static us.hebi.quickbuf.UnsafeAccess.*;

/**
 * @author Florian Enner
 * @since 20 Aug 2019
 */
public class UnsafeTest {

    TestAllTypes message;
    byte[] array;
    ByteBuffer directBuffer;

    @Before
    public void setupData() throws IOException {
        message = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        array = message.toByteArray();
        directBuffer = ByteBuffer.allocateDirect(array.length);
        directBuffer.put(array);
        directBuffer.rewind();
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
        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(null, BufferAccess.address(directBuffer), array.length);
        message.writeTo(sink);
        byte[] actual = new byte[sink.position()];
        sink.checkNoSpaceLeft();
        directBuffer.get(actual);
        assertArrayEquals(array, actual);

        // Read
        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(null, BufferAccess.address(directBuffer), array.length);
        assertEquals(message, TestAllTypes.newInstance().mergeFrom(source));
    }

    @Test
    public void testRepeatedPacked() throws IOException {
        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesPacked());
        int size = msg.getSerializedSize();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(null, BufferAccess.address(buffer), size);
        msg.writeTo(sink);
        sink.checkNoSpaceLeft();

        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(null, BufferAccess.address(buffer), size);
        assertEquals(msg, RepeatedPackables.Packed.newInstance().mergeFrom(source));
    }

    @Test
    public void testRepeatedNonPacked() throws IOException {
        RepeatedPackables.NonPacked msg = RepeatedPackables.NonPacked.parseFrom(CompatibilityTest.repeatedPackablesNonPacked());
        int size = msg.getSerializedSize();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        ProtoSink sink = ProtoSink.newUnsafeInstance().wrap(null, BufferAccess.address(buffer), size);
        msg.writeTo(sink);
        sink.checkNoSpaceLeft();

        ProtoSource source = ProtoSource.newUnsafeInstance().wrap(null, BufferAccess.address(buffer), size);
        assertEquals(msg, RepeatedPackables.NonPacked.newInstance().mergeFrom(source));
    }

    @Test
    public void testDirectBuffer() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bbSize);
        testWriteByteBuffer(ProtoSink.newUnsafeInstance(), buffer);
        testReadByteBuffer(ProtoSource.newUnsafeInstance(), buffer);
        testReadByteBuffer(ProtoSource.newUnsafeInstance(), buffer.asReadOnlyBuffer());
    }

    @Test
    public void testHeapBuffer() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bbSize);
        testWriteByteBuffer(ProtoSink.newUnsafeInstance(), buffer);
        testReadByteBuffer(ProtoSource.newUnsafeInstance(), buffer);
        testReadByteBuffer(ProtoSource.newUnsafeInstance(), buffer.asReadOnlyBuffer());
        testWriteByteBuffer(ProtoSink.newInstance(), buffer);
        testReadByteBuffer(ProtoSource.newInstance(), buffer);
        testReadByteBuffer(ProtoSource.newInstance(), buffer.asReadOnlyBuffer());
    }

    @Test
    public void testDirectBufferFailures() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bbSize);
        try {
            testWriteByteBuffer(ProtoSink.newUnsafeInstance(), buffer.asReadOnlyBuffer());
            fail("write to read-only");
        } catch (IllegalArgumentException args) {
        }

        try {
            testWriteByteBuffer(ProtoSink.newInstance(), buffer);
            fail("heap sink with direct buffer");
        } catch (IllegalArgumentException args) {
        }

        try {
            testReadByteBuffer(ProtoSource.newInstance(), buffer);
            fail("heap source with direct buffer");
        } catch (IllegalArgumentException args) {
        }
    }

    @Test
    public void testHeapBufferFailures() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bbSize);
        try {
            testWriteByteBuffer(ProtoSink.newUnsafeInstance(), buffer.asReadOnlyBuffer());
            fail("write to read-only");
        } catch (IllegalArgumentException args) {
        }

        try {
            testWriteByteBuffer(ProtoSink.newInstance(), buffer.asReadOnlyBuffer());
            fail("heap with direct buffer");
        } catch (IllegalArgumentException args) {
        }

    }

    final static int offset = 10;
    final static int bbSize = 1024;

    private ByteBuffer prepare(ByteBuffer buffer) throws IOException {
        int length = array.length;
        buffer.position(offset);
        buffer.limit(offset + length);
        return buffer;
    }

    private void testWriteByteBuffer(ProtoSink sink, ByteBuffer buffer) throws IOException {
        ByteUtil.wrapBuffer(sink, prepare(buffer));
        assertEquals(0, sink.position());
        assertEquals(buffer.remaining(), sink.spaceLeft());
        message.writeTo(sink);
        assertEquals(buffer.limit() - buffer.position(), sink.position());
        sink.checkNoSpaceLeft();
    }

    private void testReadByteBuffer(ProtoSource source, ByteBuffer buffer) throws IOException {
        ByteUtil.wrapBuffer(source, prepare(buffer));
        assertEquals(0, source.getTotalBytesRead());
        assertEquals(-1, source.getBytesUntilLimit());
        assertFalse(source.isAtEnd());
        TestAllTypes actual = TestAllTypes.parseFrom(source);
        assertEquals(buffer.remaining(), source.getTotalBytesRead());
        assertEquals(-1, source.getBytesUntilLimit());
        assertTrue(source.isAtEnd());
        assertEquals(message, actual);
    }

}
