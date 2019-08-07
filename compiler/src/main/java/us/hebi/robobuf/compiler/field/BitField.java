package us.hebi.robobuf.compiler.field;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 19 Jun 2015
 */
public class BitField {
    public static String hasBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("((bitField%d_ & 0x%08x) != 0)", intSlot, 1 << indexInSlot);
    }

    public static String setBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("bitField%d_ |= 0x%08x", intSlot, 1 << indexInSlot);
    }

    public static String clearBit(int hasBitIndex) {
        int intSlot = hasBitIndex / 32;
        int indexInSlot = hasBitIndex - (intSlot * 32);
        return String.format("bitField%d_ = (bitField%d_ & ~0x%08x)", intSlot, intSlot, 1 << indexInSlot);
    }

    public static String fieldName(int intIndex) {
        return String.format("bitField%d_", intIndex);
    }

    public static int getNumberOfFields(int fieldCount){
        return (fieldCount + 31) / 32;
    }
}
