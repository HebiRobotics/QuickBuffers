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

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class RepeatedMessage<MessageType extends ProtoMessage<MessageType>> extends RepeatedObject<RepeatedMessage<MessageType>, MessageType, MessageType> {

    @SuppressWarnings("unchecked")
    public static <T extends ProtoMessage<T>> RepeatedMessage<T> newEmptyInstance(MessageFactory<T> factory) {
        return new RepeatedMessage(factory);
    }

    private RepeatedMessage(MessageFactory<MessageType> factory) {
        if (factory == null) throw new NullPointerException();
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void copyDataFrom0(RepeatedMessage<MessageType> other) {
        for (int i = 0; i < other.length; i++) {
            array[i].copyFrom(other.array[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void setIndex0(int index, MessageType value) {
        array[index].copyFrom(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final MessageType[] allocateArray0(int desiredSize) {
        return (MessageType[]) new ProtoMessage[desiredSize];
    }

    @Override
    protected final void clearIndex0(int index) {
        array[index].clear();
    }

    public final void clearQuick() {
        for (int i = 0; i < length; i++) {
            array[i].clearQuick();
        }
        length = 0;
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
    protected final boolean isEqual(MessageType a, Object b) {
        return a.equals(b);
    }

    @Override
    protected MessageType createEmpty() {
        return factory.create();
    }

    final MessageFactory<MessageType> factory;

}
