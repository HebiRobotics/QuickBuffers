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
import org.junit.BeforeClass;
import org.junit.Test;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.TestAllTypes.NestedEnum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.*;

/**
 * Checks that the built-in JsonSource decodes messages into reused messages without allocating,
 * both for whitespace separated root values in one input and for one input per message, and
 * that reused readers, writers and sinks do not allocate either. Flaky on some JDKs, so it only
 * runs with -Dquickbuf.allocationTests=true.
 *
 * @author Florian Enner
 * @since 30 Sep 2026
 */
public class JsonAllocationTest {

    @BeforeClass
    public static void requireManualRun() {
        Assume.assumeTrue("enable with -Dquickbuf.allocationTests=true", Boolean.getBoolean("quickbuf.allocationTests"));
    }

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
        TestAllTypes msg = TestAllTypes.newInstance();
        assertNoAllocations(() -> msg.clearQuick().mergeFrom(source));
        assertEquals(1, msg.getRepeatedNestedEnum().length());
    }

    @Test
    public void testReusedSource() throws IOException {
        byte[][] inputs = split(messages());
        JsonSource source = JsonSource.newInstance().setIgnoreUnknownFields(true);
        TestAllTypes msg = TestAllTypes.newInstance();
        int[] count = {0};
        assertNoAllocations(() -> msg.clearQuick().mergeFrom(source.setInput(inputs[count[0]++ % inputs.length])));
        assertEquals(1, msg.getRepeatedNestedEnum().length());
    }

    @Test
    public void testReusedStreamSource() throws IOException {
        byte[][] inputs = split(messages());
        ByteArrayInputStream[] streams = {new ByteArrayInputStream(inputs[0]), new ByteArrayInputStream(inputs[1])};
        JsonSource source = JsonSource.newInstance().setIgnoreUnknownFields(true);
        TestAllTypes msg = TestAllTypes.newInstance();
        int[] count = {0};
        assertNoAllocations(() -> {
            ByteArrayInputStream stream = streams[count[0]++ % streams.length];
            stream.reset();
            msg.clearQuick().mergeFrom(source.setInput(stream));
        });
        assertEquals(1, msg.getRepeatedNestedEnum().length());
    }

    @Test
    public void testReusedReader() throws IOException {
        byte[][] inputs = split(messages().replace(LONG_NUMBER, "").replace(UINT64_NUMBER, "")); // more than 19 digits fall back to the JDK
        JsonReader reader = JsonReader.newInstance();
        Utf8String string = Utf8String.newEmptyInstance();
        int[] count = {0};
        assertNoAllocations(() -> readAllTokens(reader.setInput(inputs[count[0]++ % inputs.length]), string));
    }

    private static void readAllTokens(JsonReader reader, Utf8String string) throws IOException {
        int depth = 0;
        do {
            switch (reader.peek()) {
                case BEGIN_OBJECT:
                    reader.beginObject();
                    depth++;
                    break;
                case BEGIN_ARRAY:
                    reader.beginArray();
                    depth++;
                    break;
                case END_OBJECT:
                    reader.endObject();
                    depth--;
                    break;
                case END_ARRAY:
                    reader.endArray();
                    depth--;
                    break;
                case NAME:
                    reader.readName();
                    break;
                case STRING:
                    reader.readString(string);
                    break;
                case NUMBER:
                    reader.readDouble();
                    break;
                case BOOLEAN:
                    reader.readBoolean();
                    break;
                case NULL:
                    reader.readNull();
                    break;
                default:
                    throw new AssertionError("unexpected end of the input");
            }
        } while (depth > 0);
        reader.endDocument();
    }

    @Test
    public void testReusedWriter() throws IOException {
        JsonWriter writer = JsonWriter.newPrettyInstance();
        Utf8String string = Utf8String.newInstance("a\"b\\c\n\u00e9\u4e2d\ud83d\ude00");
        byte[] bytes = {0, 1, 2, (byte) 0xFF, 42};
        assertNoAllocations(() -> writer.clear().beginObject()
                .writeName("int").writeInt(-12345)
                .writeName("long").writeLong(Long.MIN_VALUE)
                .writeName("double").writeDouble(1.2345678901234567e-89)
                .writeName("float").writeFloat(1.1f)
                .writeName("chars").writeString("x\u00e9\ud83d\ude00")
                .writeName("utf8").writeString(string)
                .writeName("bytes").writeBase64(bytes, 0, bytes.length)
                .writeName("array").beginArray().writeBoolean(true).writeNull().beginObject().endObject().endArray()
                .endObject());
    }

    @Test
    public void testReusedSink() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.mergeFrom(JsonSource.newInstance(split(messages())[0]).setIgnoreUnknownFields(true));
        JsonSink sink = JsonSink.newInstance();
        assertNoAllocations(() -> sink.clear().writeMessage(msg));
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
        msg.getMutableRepeatedDouble().add(1.2345678901234567e-89).add(9007199254740993.0).add(-0.0)
                .add(Double.POSITIVE_INFINITY).add(Double.NEGATIVE_INFINITY);
        String unknown = "{\"unknownObject\":{\"a\":[1,-2.5e-3,\"x\\u00e9\",true,null,{}]}," + LONG_NUMBER;
        String names = JsonSink.newInstance().setWriteEnumsAsInts(false).writeMessage(msg).toString();
        String numbers = JsonSink.newInstance().setWriteEnumsAsInts(true).writeMessage(msg).toString();
        return unknown + names.substring(1) + "\n" + unknown + numbers.substring(1) + "\n";
    }

    private static byte[][] split(String messages) {
        String[] lines = messages.split("\n");
        return new byte[][]{lines[0].getBytes(ProtoUtil.Charsets.UTF_8), lines[1].getBytes(ProtoUtil.Charsets.UTF_8)};
    }

    private static byte[] repeat(String messages, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count / 2; i++) {
            builder.append(messages);
        }
        return builder.toString().getBytes(ProtoUtil.Charsets.UTF_8);
    }

    private static final String LONG_NUMBER = "\"unknownNumber\":12345678901234567890123,";
    private static final String UINT64_NUMBER = "\"optionalUint64\":18446744073709551614,";
    private static final int NUM_WARMUP = 5000;
    private static final int NUM_MEASURED = 500;

}
