/*-
 * #%L
 * conformance
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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
package us.hebi.quickbuf.conformance;

import com.google.quickbuf.conformance.Conformance;
import com.google.quickbuf.conformance.Conformance.ConformanceRequest;
import com.google.quickbuf.conformance.Conformance.ConformanceResponse;
import com.google.quickbuf.conformance.Conformance.TestCategory;
import com.google.quickbuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2;
import com.google.quickbuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;
import us.hebi.quickbuf.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * From Protobuf ConformanceJava
 */
public class ConformanceQuickbuf {

    public static void main(String[] args) throws Exception {
        new ConformanceQuickbuf().run();
    }

    public void run() throws Exception {
        final ProtoSource stdIn = ProtoSource.newInstance(System.in);
        final ProtoSink stdOut = ProtoSink.newInstance(System.out);

        int testCount = 0;
        final ConformanceRequest request = ConformanceRequest.newInstance();
        final ConformanceResponse response = ConformanceResponse.newInstance();
        while (!stdIn.isAtEnd()) {

            // Read request from stdin
            int rxLength = stdIn.readRawLittleEndian32();
            int limit = stdIn.pushLimit(rxLength);
            request.mergeFrom(stdIn);
            stdIn.popLimit(limit);

            // Do the test
            doTest(request, response);

            // Write to stdout
            int txLength = response.getSerializedSize();
            stdOut.writeRawLittleEndian32(txLength);
            response.writeTo(stdOut);

            // Prepare for next run
            request.clearQuick();
            response.clearQuick();
            testCount++;

        }
        String msg = String.format("ConformanceQuickbuf: received EOF from test runner after %d tests. Bytes read: %d, Bytes written: %d\n",
                testCount, stdIn.getTotalBytesRead(), stdOut.getTotalBytesWritten());
        System.err.format(msg);
    }

    private ConformanceResponse doTest(ConformanceRequest request, ConformanceResponse response) {
        String messageType = request.getMessageType();
        boolean isProto3 = messageType.equals("protobuf_test_messages.proto3.TestAllTypesProto3");
        boolean isProto2 = messageType.equals("protobuf_test_messages.proto2.TestAllTypesProto2");

        // skip unsupported modes
        if (!request.hasPayload()) throw new IllegalArgumentException("Request didn't have payload.");
        if (request.hasTextPayload()) return response.setSkipped("text");
        if (request.hasJspbPayload()) return response.setSkipped("jspb");
        if (isProto3) return response.setSkipped("proto3");

        final ProtoMessage<?> testMessage = isProto2
                ? TestAllTypesProto2.newInstance()
                : TestAllTypesProto3.newInstance();

        if (request.hasProtobufPayload()) {

            // Protobuf in
            String error = parseBinary(request.getProtobufPayload(), testMessage);
            if (error != null) return response.setParseError(error);

        } else if (request.hasJsonPayload()) {

            // JSON in
            try {
                boolean ignoreUnknown = request.getTestCategory() == TestCategory.JSON_IGNORE_UNKNOWN_PARSING_TEST;
                JsonSource source = JsonSource.newInstance(request.getJsonPayload()).setIgnoreUnknownFields(ignoreUnknown);
                testMessage.mergeFrom(source).checkInitialized();
            } catch (Throwable e) {
                return response.setSerializeError(e.getMessage());
            }

        } else {
            throw new IllegalArgumentException("Unexpected payload type");
        }

        // Protobuf out
        if (request.getRequestedOutputFormat() == Conformance.WireFormat.PROTOBUF) {
            try {
                response.getMutableProtobufPayload().copyFrom(serializeBinary(testMessage));
            } catch (IOException ioe) {
                response.setSerializeError("(" + sinkName + ") " + ioe.getMessage());
            }
            return response;
        }

        // JSON out
        if (request.getRequestedOutputFormat() == Conformance.WireFormat.JSON) {
            try {
                return response.setJsonPayload(JsonSink.newInstance()
                        .writeMessage(testMessage)
                        .toString());
            } catch (Throwable e) {
                return response.setSerializeError(e.getMessage());
            }
        }

        // Anything else
        return response.setSkipped(request.getRequestedOutputFormat().getName());
    }

    private byte[] serializeBinary(ProtoMessage<?> msg) throws IOException {
        sinkName = "DefaultSink";
        byte[] expected = msg.toByteArray();

        sinkName = "StreamSink";
        ByteArrayOutputStream baos = new ByteArrayOutputStream(expected.length);
        msg.writeTo(ProtoSink.newStreamSink().setOutput(baos));
        checkEqualBytes(expected, baos.toByteArray());

        sinkName = "BufferSink";
        ByteBuffer buffer = ByteBuffer.allocate(expected.length);
        msg.writeTo(ProtoSink.newBufferSink().setOutput(buffer));
        checkEqualBytes(expected, buffer.array());

        sinkName = "BytesSink";
        RepeatedByte bytes = RepeatedByte.newEmptyInstance().reserve(expected.length);
        msg.writeTo(ProtoSink.newBytesSink().setOutput(bytes));
        checkEqualBytes(expected, bytes.toArray());

        sinkName = "ArraySink";
        byte[] array = new byte[expected.length];
        msg.writeTo(ProtoSink.newArraySink().setOutput(array));
        checkEqualBytes(expected, array);

        sinkName = "DirectSink";
        Arrays.fill(array, (byte) 0);
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(expected.length);
        ProtoSink directSink = ProtoSink.newDirectSink();
        msg.writeTo(directSink.setOutput(directBuffer));
        directBuffer.get(array, 0, directSink.getTotalBytesWritten());
        checkEqualBytes(expected, array);

        return expected;
    }

    String sinkName = "";

    private void checkEqualBytes(byte[] expected, byte[] actual) throws IOException {
        if (!Arrays.equals(expected, actual)) {
            throw new IOException(sinkName + " encoded a different result");
        }
    }

    private String parseBinary(RepeatedByte bytes, ProtoMessage<?> msg) {
        // Sources
        byte[] array = bytes.array();
        int off = 0;
        int len = bytes.length();
        ByteBuffer byteBuffer = ByteBuffer.wrap(array, off, len);
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(len);
        directBuffer.put(array, off, len);
        directBuffer.flip();

        ProtoMessage<?> expected = parseFromSource(ProtoSource.newInstance(bytes), msg);
        ProtoMessage<?> actual = msg.clone();

        // Array source
        checkEqualDecoding(expected, actual, ProtoSource.newArraySource().setInput(bytes));
        checkEqualDecoding(expected, actual, ProtoSource.newArraySource().setInput(array, 0, len));
        checkEqualDecoding(expected, actual, ProtoSource.newArraySource().setInput(byteBuffer.duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newArraySource().setInput(byteBuffer.asReadOnlyBuffer().duplicate()));

        // Direct source
        checkEqualDecoding(expected, actual, ProtoSource.newDirectSource().setInput(bytes));
        checkEqualDecoding(expected, actual, ProtoSource.newDirectSource().setInput(array, 0, len));
        checkEqualDecoding(expected, actual, ProtoSource.newDirectSource().setInput(byteBuffer.duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newDirectSource().setInput(byteBuffer.asReadOnlyBuffer().duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newDirectSource().setInput(directBuffer.duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newDirectSource().setInput(directBuffer.asReadOnlyBuffer().duplicate()));

        // Buffer source
        checkEqualDecoding(expected, actual, ProtoSource.newBufferSource().setInput(byteBuffer.duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newBufferSource().setInput(byteBuffer.asReadOnlyBuffer().duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newBufferSource().setInput(directBuffer.duplicate()));
        checkEqualDecoding(expected, actual, ProtoSource.newBufferSource().setInput(directBuffer.asReadOnlyBuffer().duplicate()));

        // Stream source
        checkEqualDecoding(expected, actual, ProtoSource.newStreamSource().setInput(new ByteArrayInputStream(array, off, len)));

        return parseError;
    }

    private void checkEqualDecoding(ProtoMessage<?> expected, ProtoMessage<?> actual, ProtoSource source) {
        actual = parseFromSource(source, actual);
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException(source.getClass().getSimpleName() + " did not parse a matching result");
        }
    }

    private ProtoMessage<?> parseFromSource(ProtoSource source, ProtoMessage<?> msg) {
        parseError = null;
        try {
            return msg.clearQuick().mergeFrom(source).checkInitialized();
        } catch (Throwable e) {
            parseError = e.getMessage();
            return null;
        }
    }

    private String parseError;

}
