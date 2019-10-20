// Protocol Buffers - Google's data interchange format
// Copyright 2013 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package us.hebi.robobuf;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * ---- Code below was adapted from Javanano's MessageNano ----
 *
 * Abstract interface implemented by Protocol Message objects.
 *
 * @author wink@google.com Wink Saville
 */
public abstract class ProtoMessage<MessageType extends ProtoMessage> {

    private static final long serialVersionUID = 0L;
    protected volatile int cachedSize = -1;

    // Keep the first two bitfields in the parent class so that they
    // are in the same cache line as the object header
    protected int bitField0_;
    protected int bitField1_;

    /**
     * Get the number of bytes required to encode this message.
     * Returns the cached size or calls getSerializedSize which
     * sets the cached size. This is used internally when serializing
     * so the size is only computed once. If a member is modified
     * then this could be stale call getSerializedSize if in doubt.
     */
    public int getCachedSize() {
        if (cachedSize < 0) {
            // getSerializedSize sets cachedSize
            getSerializedSize();
        }
        return cachedSize;
    }

    /**
     * Computes the number of bytes required to encode this message.
     * The size is cached and the cached result can be retrieved
     * using getCachedSize().
     */
    public int getSerializedSize() {
        int size = computeSerializedSize();
        cachedSize = size;
        return size;
    }

    /**
     * Copies all fields and data from another message of the same
     * type into this message.
     *
     * @param other
     * @return this
     */
    public abstract MessageType copyFrom(MessageType other);

    /**
     * Sets all fields and data to their default values. Does not
     * get rid of memory that was allocated.
     *
     * @return this
     */
    public abstract MessageType clear();

    /**
     * Clears all has state so that the message would serialize empty,
     * but does not set field default values and does not get rid of
     * memory that was allocated for repeated types.
     *
     * Use this if you use this message for serialization purposes or
     * if you do not require default values for unset fields.
     *
     * @return this
     */
    public MessageType clearQuick() {
        return clear();
    }

    /**
     * Computes the number of bytes required to encode this message. This does not update the
     * cached size.
     */
    protected abstract int computeSerializedSize();

    /**
     * Serializes the message and writes it to {@code output}.
     *
     * @param output the output to receive the serialized form.
     * @throws IOException if an error occurred writing to {@code output}.
     */
    public abstract void writeTo(ProtoSink output) throws IOException;

    /**
     * Parse {@code input} as a message of this type and merge it with the
     * message being built.
     */
    public abstract ProtoMessage mergeFrom(ProtoSource input) throws IOException;

    /**
     * Serialize to a byte array.
     *
     * @return byte array with the serialized data.
     */
    public final byte[] toByteArray() {
        return ProtoMessage.toByteArray(this);
    }

    /**
     * Serialize to a byte array.
     *
     * @return byte array with the serialized data.
     */
    public static final byte[] toByteArray(ProtoMessage msg) {
        final byte[] result = new byte[msg.getSerializedSize()];
        toByteArray(msg, result, 0, result.length);
        return result;
    }

    /**
     * Serialize to a byte array starting at offset through length. The
     * method getSerializedSize must have been called prior to calling
     * this method so the proper length is know.  If an attempt to
     * write more than length bytes OutOfSpaceException will be thrown
     * and if length bytes are not written then IllegalStateException
     * is thrown.
     */
    public static final void toByteArray(ProtoMessage msg, byte[] data, int offset, int length) {
        try {
            final ProtoSink output = ProtoSink.newInstance(data, offset, length);
            msg.writeTo(output);
            output.checkNoSpaceLeft();
        } catch (IOException e) {
            throw new RuntimeException("Serializing to a byte array threw an IOException "
                    + "(should never happen).", e);
        }
    }

    /**
     * Parse {@code data} as a message of this type and merge it with the
     * message being built.
     */
    public static final <T extends ProtoMessage> T mergeFrom(T msg, final byte[] data)
            throws InvalidProtocolBufferException {
        return mergeFrom(msg, data, 0, data.length);
    }

    /**
     * Parse {@code data} as a message of this type and merge it with the
     * message being built.
     */
    public static final <T extends ProtoMessage> T mergeFrom(T msg, final byte[] data,
                                                             final int off, final int len) throws InvalidProtocolBufferException {
        try {
            final ProtoSource input = ProtoSource.newInstance(data, off, len);
            msg.mergeFrom(input);
            input.checkLastTagWas(0);
            return msg;
        } catch (InvalidProtocolBufferException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Reading from a byte array threw an IOException (should "
                    + "never happen).");
        }
    }

    /**
     * Returns a string that is (mostly) compatible with ProtoBuffer's TextFormat. Note that groups
     * (which are deprecated) are not serialized with the correct field name.
     *
     * <p>This is implemented using reflection, so it is not especially fast nor is it guaranteed
     * to find all fields if you have method removal turned on for proguard.
     */
    @Override
    public String toString() {
        return MessageNanoPrinter.print(this);
    }

    /** Provides support for cloning. This only works if you specify the generate_clone method. */
    @Override
    public ProtoMessage clone() throws CloneNotSupportedException {
        return (ProtoMessage) super.clone();
    }

    /**
     * Helper to determine the default value for 'Bytes' fields. The Protobuf
     * generator encodes raw bytes as strings with ISO-8859-1 encoding.
     */
    protected static byte[] bytesDefaultValue(String bytes) {
        return bytes.getBytes(Charsets.ISO_8859_1);
    }

    private static class Charsets {
        private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    }

}
