/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.quickbuf;

import java.io.IOException;

/**
 * Abstract interface implemented by Protocol Message objects.
 *
 * API partially copied from Google's MessageNano
 *
 * @author Florian Enner
 */
public abstract class ProtoMessage<MessageType extends ProtoMessage> {

    private static final long serialVersionUID = 0L;
    protected volatile int cachedSize = -1;

    // Keep the first bitfield in the parent class so that it
    // is likely in the same cache line as the object header
    protected int bitField0_;

    // Allow storing unknown bytes so that messages can be routed
    // without having full knowledge of the definition
    protected final RepeatedByte unknownBytes;
    protected static final byte[] unknownBytesJsonKey = {'\"', 'u', 'n', 'k', 'n', 'o', 'w', 'n', 'B', 'y', 't', 'e', 's', '\"', ':'};

    protected ProtoMessage() {
        this(false);
    }

    protected ProtoMessage(boolean storeUnknownBytes) {
        this.unknownBytes = storeUnknownBytes ? RepeatedByte.newEmptyInstance() : null;
    }

    /**
     * @return binary representation of all fields with tags that could not be parsed
     */
    public final RepeatedByte getUnknownBytes() {
        if (unknownBytes == null) {
            throw new IllegalStateException("Storing unknown bytes is not enabled");
        }
        return unknownBytes;
    }

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
     * Returns true if all required fields in the message and all embedded
     * messages are set, false otherwise.
     */
    public boolean isInitialized() {
        return true;
    }

    /**
     * Computes the number of bytes required to encode this message. This does
     * not update the cached size.
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
     * Serializes this message in a JSON format compatible with
     * https://developers.google.com/protocol-buffers/docs/proto3#json.
     *
     * The implementation may not fail on missing required fields, so
     * required fields would need to be checked using {@link ProtoMessage#isInitialized()}.
     *
     * @param output json sink
     */
    public void writeTo(final JsonSink output) {
        throw new IllegalStateException("Generated message does not implement JSON output");
    }

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
            final ProtoSink output = ProtoSink.newSafeInstance().wrap(data, offset, length);
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
     * Indicates whether another object is "equal to" this one.
     *
     * An object is considered equal when it is of the same message
     * type, contains the same fields (same has state), and all of
     * the field contents are equal.
     *
     * This comparison ignores unknown fields, so the serialized binary
     * form may not be equal.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * Messages have no immutable state and should not
     * be used in hashing structures. This implementation
     * returns a constant value in order to satisfy the
     * contract.
     */
    @Override
    public final int hashCode() {
        return 0;
    }

    /**
     * Returns a string that contains a human readable representation of the contents. The output
     * may not be compatible with any existing readers.
     */
    @Override
    public String toString() {
        return JsonSink.newPrettyInstance()
                .reserve(getSerializedSize() * 8)
                .writeMessage(this)
                .toString();
    }

    /** Provides support for cloning if the method is generated for child classes */
    @Override
    public ProtoMessage clone() throws CloneNotSupportedException {
        return (ProtoMessage) super.clone();
    }

    /**
     * Helper to determine the default value for 'Bytes' fields. The Protobuf
     * generator encodes raw bytes as strings with ISO-8859-1 encoding.
     */
    protected static byte[] bytesDefaultValue(String bytes) {
        return bytes.getBytes(ProtoUtil.Charsets.ISO_8859_1);
    }

}
