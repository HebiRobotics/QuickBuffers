package us.hebi.robobuf.compiler;

/**
 * Utilities for creating protobuf-like bit-sets to keep has state
 *
 * @author Florian Enner
 * @since 19 Jun 2015
 */
class BitField {

    static String hasBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("((bitField%d_ & 0x%08x) != 0)", intSlot, 1 << indexInSlot);
    }

    static String setBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("bitField%d_ |= 0x%08x", intSlot, 1 << indexInSlot);
    }

    static String clearBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("bitField%d_ = (bitField%d_ & ~0x%08x)", intSlot, intSlot, 1 << indexInSlot);
    }

    /**
     * string that results in 1 if the bit is set, or zero if it is not set
     *
     * Initial tests for using this as an optimization for removing if statements
     * for fixed width types had no effect. The JIT probably already spits out
     * similar code anyways.
     *
     * @param hasBitIndex
     * @return
     */
    static String oneOrZeroBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("((bitField%d_ & 0x%08x) >>> %d)", intSlot, 1 << indexInSlot, indexInSlot);
    }

    static String fieldName(int intIndex) {
        return String.format("bitField%d_", intIndex);
    }

    static int getNumberOfFields(int fieldCount) {
        return (fieldCount + 31) / 32;
    }

}
