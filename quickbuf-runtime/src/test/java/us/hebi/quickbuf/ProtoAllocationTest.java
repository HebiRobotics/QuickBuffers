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

import org.junit.Assume;
import org.junit.Test;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.TestAllTypes.NestedMessage;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.*;

/**
 * Checks that reused messages, sources and sinks read, write and copy binary messages without allocating
 *
 * @author Florian Enner
 * @since 28 Oct 2026
 */
public class ProtoAllocationTest {

    @Test
    public void testReadWrite() throws IOException {
        byte[][] inputs = inputs();
        ProtoSource source = ProtoSource.newInstance(inputs[0]);
        byte[] output = new byte[2 * inputs[0].length];
        ProtoSink sink = ProtoSink.newInstance(output);
        TestAllTypes msg = TestAllTypes.newInstance();
        int[] count = {0};
        assertNoAllocations(() -> {
            msg.clearQuick().mergeFrom(source.setInput(inputs[count[0]++ % inputs.length]));
            msg.writeTo(sink.setOutput(output));
        });
        assertEquals(2, msg.getRepeatedString().length());
    }

    @Test
    public void testCopyFrom() throws IOException {
        byte[][] inputs = inputs();
        TestAllTypes[] messages = {TestAllTypes.parseFrom(inputs[0]), TestAllTypes.parseFrom(inputs[1])};
        TestAllTypes msg = TestAllTypes.newInstance();
        int[] count = {0};
        assertNoAllocations(() -> msg.clear().copyFrom(messages[count[0]++ % messages.length]));
        assertNoAllocations(() -> msg.clearQuick().mergeFrom(messages[count[0]++ % messages.length]));
    }

    @Test
    public void testOnlyUnknownFields() throws IOException {
        byte[][] inputs = inputs();
        ProtoSource source = ProtoSource.newInstance(inputs[0]);
        NestedMessage msg = NestedMessage.newInstance();
        int[] count = {0};
        assertNoAllocations(() -> msg.clearQuick().mergeFrom(source.setInput(inputs[count[0]++ % inputs.length])));
        assertTrue(msg.isEmpty());
        assertEquals(inputs[0].length, msg.getUnknownBytes().length());
    }

    /**
     * Two messages with different strings, so string fields can't keep their decoded values
     */
    private static byte[][] inputs() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage()).clearOptionalInt32();
        byte[][] inputs = new byte[2][];
        for (int i = 0; i < inputs.length; i++) {
            msg.setOptionalString("optional " + i);
            msg.getMutableRepeatedString().clear().addAll(new String[]{"first " + i, "second \u00e9\u4e2d " + i});
            inputs[i] = msg.toByteArray();
        }
        return inputs;
    }

    interface Action {
        void run() throws IOException;
    }

    private static void assertNoAllocations(Action action) throws IOException {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Assume.assumeTrue("allocation counters are not available", isAllocationCounterAvailable(threads));
        com.sun.management.ThreadMXBean allocations = (com.sun.management.ThreadMXBean) threads;
        final long thread = Thread.currentThread().getId();

        for (int i = 0; i < NUM_WARMUP; i++) {
            action.run();
        }
        long overhead = -allocations.getThreadAllocatedBytes(thread);
        overhead += allocations.getThreadAllocatedBytes(thread);
        long allocated = -allocations.getThreadAllocatedBytes(thread);
        for (int i = 0; i < NUM_MEASURED; i++) {
            action.run();
        }
        allocated += allocations.getThreadAllocatedBytes(thread);
        assertEquals("allocated bytes", 0, allocated - overhead);
    }

    private static boolean isAllocationCounterAvailable(ThreadMXBean threads) {
        try {
            return threads instanceof com.sun.management.ThreadMXBean
                    && ((com.sun.management.ThreadMXBean) threads).isThreadAllocatedMemorySupported()
                    && ((com.sun.management.ThreadMXBean) threads).isThreadAllocatedMemoryEnabled();
        } catch (LinkageError notHotSpot) {
            return false;
        }
    }

    private static final int NUM_WARMUP = 5000;
    private static final int NUM_MEASURED = 500;

}
