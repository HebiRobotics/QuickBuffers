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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract interface implemented by Protocol Message objects.
 * <p>
 * API partially copied from Google's MessageNano
 *
 * @author Florian Enner
 */
public abstract class ProtoMessage<MessageType extends ProtoMessage<?>> {

    private static final long serialVersionUID = 0L;
    protected int cachedSize = -1;

    // Keep the first bitfield in the parent class so that it
    // is likely in the same cache line as the object header
    protected int bitField0_;

    protected ProtoMessage() {
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
     * <p>
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
     * Helper method to check if this message is initialized, i.e.,
     * if all required fields are set.
     *
     * Message content is not automatically checked after merging
     * new data. This method should be called manually as needed.
     *
     * @throws InvalidProtocolBufferException if it is not initialized.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final MessageType checkInitialized() throws InvalidProtocolBufferException {
        if (!isInitialized()) {
            throw new UninitializedMessageException(this)
                    .asInvalidProtocolBufferException();
        }
        return (MessageType) this;
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
    public abstract MessageType mergeFrom(ProtoSource input) throws IOException;

    /**
     * Merge {@code other} into the message being built. {@code other} must have the exact same type
     * as {@code this}.
     *
     * <p>Merging occurs as follows. For each field:<br>
     * * For singular primitive fields, if the field is set in {@code other}, then {@code other}'s
     * value overwrites the value in this message.<br>
     * * For singular message fields, if the field is set in {@code other}, it is merged into the
     * corresponding sub-message of this message using the same merging rules.<br>
     * * For repeated fields, the elements in {@code other} are concatenated with the elements in
     * this message.<br>
     * * For oneof groups, if the other message has one of the fields set, the group of this message
     * is cleared and replaced by the field of the other message, so that the oneof constraint is
     * preserved.
     *
     * <p>This is equivalent to the {@code Message::MergeFrom} method in C++.
     */
    public MessageType mergeFrom(MessageType other) {
        throw new RuntimeException("MergeFrom method not generated");
    }

    /**
     * Serializes this message in a JSON format compatible with
     * https://developers.google.com/protocol-buffers/docs/proto3#json.
     * <p>
     * The implementation may not fail on missing required fields, so
     * required fields would need to be checked using {@link ProtoMessage#isInitialized()}.
     *
     * @param output json sink
     */
    public void writeTo(final AbstractJsonSink output) throws IOException {
        throw new IllegalStateException("Generated message does not implement JSON output");
    }

    public MessageType mergeFrom(final AbstractJsonSource input) throws IOException {
        throw new IllegalStateException("Generated message does not implement JSON input");
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
    public static byte[] toByteArray(ProtoMessage<?> msg) {
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
    public static void toByteArray(ProtoMessage<?> msg, byte[] data, int offset, int length) {
        try {
            final ProtoSink output = ProtoSink.newArraySink().setOutput(data, offset, length);
            msg.writeTo(output);
            output.checkNoSpaceLeft();
        } catch (IOException e) {
            throw new RuntimeException("Serializing to a byte array threw an IOException "
                    + "(should never happen).", e);
        }
    }

    /**
     * Parse {@code data} as a message of this type and merge it with the message being built.
     */
    public static final <T extends ProtoMessage> T mergeFrom(T msg, final byte[] data) throws InvalidProtocolBufferException {
        return mergeFrom(msg, data, 0, data.length);
    }

    /**
     * Parse {@code data} as a message of this type and merge it with the message being built.
     */
    public static final <T extends ProtoMessage> T mergeFrom(T msg, final byte[] data, final int off, final int len)
            throws InvalidProtocolBufferException {
        try {
            return ProtoMessage.mergeFrom(msg, ProtoSource.newInstance(data, off, len));
        } catch (InvalidProtocolBufferException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Reading from a byte array threw an IOException (should never happen).");
        }
    }

    /**
     * Parse {@code data} as a message of this type and merge it with the message being built.
     */
    public static <T extends ProtoMessage> T mergeFrom(T msg, ProtoSource input) throws IOException {
        msg.mergeFrom(input);
        input.checkLastTagWas(0);
        return msg;
    }

    /**
     * Indicates whether another object is "equal to" this one.
     * <p>
     * An object is considered equal when it is of the same message
     * type, contains the same fields (same has state), and all of
     * the field contents are equal.
     * <p>
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
                .writeMessage(this)
                .toString();
    }

    /**
     * Provides support for cloning if the method is generated for child classes
     */
    @Override
    public ProtoMessage clone() throws CloneNotSupportedException {
        return (ProtoMessage) super.clone();
    }

    /**
     * @return the full path to all missing required fields in the message
     */
    public List<String> getMissingFields(){
        List<String> results = new ArrayList<String>();
        getMissingFields("", results);
        return results;
    }

    /**
     * Adds the full path to all missing required fields in the message
     */
    protected void getMissingFields(String prefix, List<String> results) {
    }

    protected static void getMissingFields(String prefix, String fieldName, ProtoMessage<?> field, List<String> results) {
        if (!field.isInitialized()) {
            field.getMissingFields(prefix + fieldName + ".", results);
        }
    }

    protected static void getMissingFields(String prefix, String fieldName, RepeatedMessage<?> field, List<String> results) {
        for (int i = 0; i < field.length; i++) {
            if (!field.array[i].isInitialized()) {
                field.array[i].getMissingFields(prefix + fieldName + "[" + i + "].", results);
            }
        }
    }

    /**
     * Helper to determine the default value for 'Bytes' fields. The Protobuf
     * generator encodes raw bytes as strings with ISO-8859-1 encoding.
     */
    protected static byte[] bytesDefaultValue(String bytes) {
        return bytes.getBytes(ProtoUtil.Charsets.ISO_8859_1);
    }

    /**
     * @return binary representation of all fields with tags that could not be parsed
     */
    public RepeatedByte getUnknownBytes() {
        throw new IllegalStateException("Support for unknown bytes has not been generated.");
    }

    /**
     * JSON field name for serializing unknown bytes
     */
    protected static final FieldName unknownBytesFieldName = FieldName.forField("unknownBytes");

}
