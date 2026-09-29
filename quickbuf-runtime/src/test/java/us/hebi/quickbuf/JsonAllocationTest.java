/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2026 HEBI Robotics
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
import protos.test.quickbuf.TestAllTypes.NestedEnum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.*;

/**
 * Checks that the built-in JsonSource decodes messages into reused messages without allocating.
 * The source gets reused by reading whitespace separated root values one after another.
 *
 * @author Florian Enner
 * @since 30 Sep 2026
 */
public class JsonAllocationTest {

    @Test
    public void testByteArrayInput() throws IOException {
        byte[] input = repeat(messages(), NUM_WARMUP + NUM_MEASURED);
        assertNoAllocations(JsonSource.newInstance(input).setIgnoreUnknownFields(true));
    }

    @Test
    public void testInputStream() throws IOException {
        byte[] input = repeat(messages(), NUM_WARMUP + NUM_MEASURED);
        assertNoAllocations(JsonSource.newInstance(new ByteArrayInputStream(input)).setIgnoreUnknownFields(true));
    }

    private static void assertNoAllocations(JsonSource source) throws IOException {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Assume.assumeTrue("allocation counters are not available", isAllocationCounterAvailable(threads));
        com.sun.management.ThreadMXBean allocations = (com.sun.management.ThreadMXBean) threads;
        final long thread = Thread.currentThread().getId();

        TestAllTypes msg = TestAllTypes.newInstance();
        for (int i = 0; i < NUM_WARMUP; i++) {
            msg.clearQuick().mergeFrom(source);
        }
        long overhead = -allocations.getThreadAllocatedBytes(thread);
        overhead += allocations.getThreadAllocatedBytes(thread);
        long allocated = -allocations.getThreadAllocatedBytes(thread);
        for (int i = 0; i < NUM_MEASURED; i++) {
            msg.clearQuick().mergeFrom(source);
        }
        allocated += allocations.getThreadAllocatedBytes(thread);
        assertEquals("allocated bytes", 0, allocated - overhead);
        assertEquals(1, msg.getRepeatedNestedEnum().length());
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

    /**
     * Every field type with enums as names and as numbers, plus unknown fields that get skipped
     */
    private static String messages() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage())
                .setOptionalDouble(0.1 + 0.2)
                .setOptionalFloat(1.1f)
                .setOptionalInt64(-1234567890123L)
                .setOptionalUint64(-2) // above 2^63
                .setOptionalString("a\"b\\c\n\u00e9\u4e2d\ud83d\ude00")
                .setOptionalBytes(new byte[]{0, 1, 2, (byte) 0xFF, 42})
                .setOptionalNestedEnum(NestedEnum.BAZ);
        msg.getMutableRepeatedNestedEnum().clear().add(NestedEnum.FOO);
        msg.getMutableRepeatedDouble().add(1.2345678901234567e-89).add(9007199254740993.0).add(-0.0);
        String unknown = "{\"unknownObject\":{\"a\":[1,-2.5e-3,\"x\\u00e9\",true,null,{}]},\"unknownNumber\":12345678901234567890123,";
        String names = JsonSink.newInstance().setWriteEnumsAsInts(false).writeMessage(msg).toString();
        String numbers = JsonSink.newInstance().setWriteEnumsAsInts(true).writeMessage(msg).toString();
        return unknown + names.substring(1) + "\n" + unknown + numbers.substring(1) + "\n";
    }

    private static byte[] repeat(String messages, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count / 2; i++) {
            builder.append(messages);
        }
        return builder.toString().getBytes(ProtoUtil.Charsets.UTF_8);
    }

    private static final int NUM_WARMUP = 5000;
    private static final int NUM_MEASURED = 500;

}
