package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedMessage<MessageType extends ProtoMessage<MessageType>> extends RepeatedObject<RepeatedMessage<MessageType>, MessageType, MessageType> {

    public RepeatedMessage(MessageFactory<MessageType> factory) {
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
