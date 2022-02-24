/*-
 * #%L
 * quickbuf-generator
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

package us.hebi.quickbuf.generator;

import us.hebi.quickbuf.generator.RequestInfo.FieldInfo;

import java.util.List;

/**
 * Utilities for creating protobuf-like bit-sets to keep has state
 *
 * @author Florian Enner
 * @since 19 Jun 2015
 */
class BitField {

    static int BITS_PER_FIELD = 32;

    static int getFieldIndex(int fieldIndex) {
        return fieldIndex / BITS_PER_FIELD;
    }

    static int getBitIndex(int fieldIndex) {
        return fieldIndex % BITS_PER_FIELD;
    }

    static String hasBit(int hasBitIndex) {
        return String.format("(bitField%d_ & 0x%08x) != 0", getFieldIndex(hasBitIndex), 1 << getBitIndex(hasBitIndex));
    }

    static String setBit(int hasBitIndex) {
        return String.format("bitField%d_ |= 0x%08x", getFieldIndex(hasBitIndex), 1 << getBitIndex(hasBitIndex));
    }

    static String clearBit(int hasBitIndex) {
        int field = getFieldIndex(hasBitIndex);
        return String.format("bitField%d_ &= ~0x%08x", field, 1 << getBitIndex(hasBitIndex));
    }

    static String hasNoBits(int numBitFields) {
        String output = "((" + fieldName(0);
        for (int i = 1; i < numBitFields; i++) {
            output += " | " + fieldName(i);
        }
        return output + ") == 0)";
    }

    static String isMissingAnyBit(int[] bitset) {
        StringBuilder builder = new StringBuilder();
        int usedFields = 0;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i] == 0) continue;
            builder.append(usedFields++ == 0 ? "(" : " || ");
            builder.append(String.format("((bitField%d_ & 0x%08x) != 0x%08x)",
                    i, bitset[i], bitset[i]));
        }
        builder.append(usedFields > 0 ? ")" : "true");
        return builder.toString();
    }

    static String hasAnyBit(int[] bitset) {
        StringBuilder builder = new StringBuilder();
        int usedFields = 0;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i] == 0) continue;
            builder.append(usedFields++ == 0 ? "((" : " | ");
            builder.append(String.format("(bitField%d_ & 0x%08x)", i, bitset[i]));
        }
        builder.append(usedFields > 0 ? ") != 0)" : "true");
        return builder.toString();
    }

    static int[] generateBitset(List<FieldInfo> fields) {
        int maxIndex = fields.stream().mapToInt(FieldInfo::getBitIndex).max().orElse(0);
        int[] bits = new int[getNumberOfFields(maxIndex) + 1];
        for (FieldInfo field : fields) {
            int fieldIndex = getFieldIndex(field.getBitIndex());
            bits[fieldIndex] |= 1 << getBitIndex(field.getBitIndex());
        }
        return bits;
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
        int intSlot = hasBitIndex / BITS_PER_FIELD;
        int indexInSlot = hasBitIndex - (intSlot * BITS_PER_FIELD);
        return String.format("((bitField%d_ & 0x%08x) >>> %d)", intSlot, 1 << indexInSlot, indexInSlot);
    }

    static String fieldName(int intIndex) {
        return String.format("bitField%d_", intIndex);
    }

    static int getNumberOfFields(int fieldCount) {
        return (fieldCount + (BITS_PER_FIELD - 1)) / BITS_PER_FIELD;
    }

}
