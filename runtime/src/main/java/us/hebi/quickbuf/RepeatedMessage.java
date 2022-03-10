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

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class RepeatedMessage<MessageType extends ProtoMessage<MessageType>> extends RepeatedObject<RepeatedMessage<MessageType>, MessageType, MessageType, MessageType> {

    @SuppressWarnings("unchecked")
    public static <T extends ProtoMessage<T>> RepeatedMessage<T> newEmptyInstance(MessageFactory<T> factory) {
        return new RepeatedMessage(factory);
    }

    private RepeatedMessage(MessageFactory<MessageType> factory) {
        this.factory = ProtoUtil.checkNotNull(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void setIndex0(int index, MessageType value) {
        array[index].copyFrom(value);
    }

    @Override
    protected MessageType getIndex0(int index) {
        return array[index];
    }

    @Override
    protected final void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected void copyFrom0(MessageType store, MessageType other) {
        store.copyFrom(other);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final MessageType[] allocateArray0(int desiredSize) {
        return (MessageType[]) new ProtoMessage[desiredSize];
    }

    public final RepeatedMessage<MessageType> clearQuick() {
        for (int i = 0; i < length; i++) {
            array[i].clearQuick();
        }
        length = 0;
        return this;
    }

    /**
     * @return true if all contained messages are initialized
     */
    public final boolean isInitialized() {
        for (int i = 0; i < length; i++) {
            if (!array[i].isInitialized())
                return false;
        }
        return true;
    }

    @Override
    protected MessageType createEmpty() {
        return factory.create();
    }

    final MessageFactory<MessageType> factory;

}
