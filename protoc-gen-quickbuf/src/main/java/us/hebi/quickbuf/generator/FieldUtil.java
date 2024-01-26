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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;

import java.util.Comparator;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class FieldUtil {

    private static final int WIRETYPE_VARINT = 0;
    private static final int WIRETYPE_FIXED64 = 1;
    private static final int WIRETYPE_LENGTH_DELIMITED = 2;
    private static final int WIRETYPE_START_GROUP = 3;
    private static final int WIRETYPE_END_GROUP = 4;
    private static final int WIRETYPE_FIXED32 = 5;
    private static final int TAG_TYPE_BITS = 3;
    private static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

    static int makeTag(FieldDescriptorProto descriptor) {
        return descriptor.getNumber() << TAG_TYPE_BITS | getWireType(descriptor.getType());
    }

    static int makePackedTag(FieldDescriptorProto descriptor) {
        return descriptor.getNumber() << 3 | WIRETYPE_LENGTH_DELIMITED;
    }

    static int makeGroupEndTag(int startTag) {
        int fieldId = startTag >>> TAG_TYPE_BITS;
        return fieldId << TAG_TYPE_BITS | WIRETYPE_END_GROUP;
    }

    /**
     * Compute the number of bytes that would be needed to encode a varint.
     * {@code value} is treated as unsigned, so it won't be sign-extended if
     * negative.
     */
    static int computeRawVarint32Size(final int value) {
        if ((value & (0xffffffff << 7)) == 0) return 1;
        if ((value & (0xffffffff << 14)) == 0) return 2;
        if ((value & (0xffffffff << 21)) == 0) return 3;
        if ((value & (0xffffffff << 28)) == 0) return 4;
        return 5;
    }

    /**
     * OneOf bits and required bits should be close together so that it is more likely for them to be checked in
     * a single bit comparison. This sorter groups all required fields, followed by OneOf groups, followed by
     * everything else.
     */
    static final Comparator<FieldDescriptorProto> GroupOneOfAndRequiredBits = (o1, o2) -> {
        int n1 = o1.hasOneofIndex() ? o1.getOneofIndex() : (o1.getLabel() == Label.LABEL_REQUIRED) ? Integer.MAX_VALUE : -1;
        int n2 = o2.hasOneofIndex() ? o2.getOneofIndex() : (o2.getLabel() == Label.LABEL_REQUIRED) ? Integer.MAX_VALUE : -1;
        return n2 - n1;
    };

    /**
     * Sort fields according to their specified field number. This is used as the serialization order
     * by Google's protobuf bindings.
     */
    static final Comparator<FieldGenerator> AscendingNumberSorter = Comparator.comparingInt(field -> field.getInfo().getNumber());

    /**
     * Sort the fields according to their layout in memory.
     * <p>
     * Summary:
     * - Objects are 8 bytes aligned in memory (address A is K aligned if A % K == 0)
     * - All fields are type aligned (long/double is 8 aligned, integer/float 4, short/char 2)
     * - Fields are packed in the order of their size, except for references which are last
     * - Classes fields are never mixed, so if B extends A, A's fields will be laid out first
     * - Sub class fields start at a 4 byte alignment
     * - If the first field of a class is long/double and the class starting point (after header, or after super) is not 8 aligned then a smaller field may be swapped to fill in the 4 bytes gap.
     * <p>
     * For more info, see
     * http://psy-lob-saw.blogspot.com/2013/05/know-thy-java-object-memory-layout.html
     */
    static final Comparator<FieldDescriptorProto> MemoryLayoutSorter = (objA, objB) -> {
        // The higher the number, the closer to the beginning
        int weightA = getSortingWeight(objA) + (objA.getLabel() == Label.LABEL_REPEATED ? -50 : 0);
        int weightB = getSortingWeight(objB) + (objB.getLabel() == Label.LABEL_REPEATED ? -50 : 0);
        if (weightA == weightB) {
            // Higher field number -> lower ranking
            return objA.getNumber() - objB.getNumber();
        }
        return weightB - weightA;
    };

    private static int getSortingWeight(FieldDescriptorProto descriptor) {
        // Start with largest width and get smaller. References come at the end.
        // For memory layout we only need to look at the base type, but it
        // can't hurt to sort by exact type to maybe keep the serialization code
        // in cache hot.
        int weight = 0;
        switch (descriptor.getType()) {

            case TYPE_DOUBLE:
                weight++;
            case TYPE_FIXED64:
                weight++;
            case TYPE_SFIXED64:
                weight++;

            case TYPE_INT64:
                weight++;
            case TYPE_UINT64:
                weight++;
            case TYPE_SINT64:
                weight++;

            case TYPE_FLOAT:
                weight++;
            case TYPE_FIXED32:
                weight++;
            case TYPE_SFIXED32:
                weight++;

            case TYPE_INT32:
                weight++;
            case TYPE_UINT32:
                weight++;
            case TYPE_SINT32:
                weight++;

            case TYPE_ENUM:
                weight++;

            case TYPE_BOOL:
                weight++;

            case TYPE_MESSAGE:
                weight++;
            case TYPE_GROUP:
                weight++;

            case TYPE_BYTES:
                weight++;

            case TYPE_STRING:
                weight++;

                return weight;
            default:
                throw new IllegalStateException("Unexpected value: " + descriptor.getType());
        }
    }

    private static int getWireType(FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_UINT64:
            case TYPE_INT64:
            case TYPE_UINT32:
            case TYPE_INT32:
            case TYPE_BOOL:
            case TYPE_ENUM:
            case TYPE_SINT32:
            case TYPE_SINT64:
                return WIRETYPE_VARINT;

            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_DOUBLE:
                return WIRETYPE_FIXED64;

            case TYPE_BYTES:
            case TYPE_STRING:
            case TYPE_MESSAGE:
                return WIRETYPE_LENGTH_DELIMITED;

            case TYPE_GROUP:
                return WIRETYPE_START_GROUP;

            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_FLOAT:
                return WIRETYPE_FIXED32;

        }
        throw new GeneratorException("Unsupported type: " + type);
    }

    /**
     * Used for e.g. writeUInt64() methods
     *
     * @param type
     * @return
     */
    static String getCapitalizedType(FieldDescriptorProto.Type type) {
        switch (type) {
            case TYPE_DOUBLE:
                return "Double";
            case TYPE_FLOAT:
                return "Float";
            case TYPE_INT64:
                return "Int64";
            case TYPE_UINT64:
                return "UInt64";
            case TYPE_INT32:
                return "Int32";
            case TYPE_FIXED64:
                return "Fixed64";
            case TYPE_FIXED32:
                return "Fixed32";
            case TYPE_BOOL:
                return "Bool";
            case TYPE_STRING:
                return "String";
            case TYPE_GROUP:
                return "Group";
            case TYPE_MESSAGE:
                return "Message";
            case TYPE_BYTES:
                return "Bytes";
            case TYPE_UINT32:
                return "UInt32";
            case TYPE_ENUM:
                return "Enum";
            case TYPE_SFIXED32:
                return "SFixed32";
            case TYPE_SFIXED64:
                return "SFixed64";
            case TYPE_SINT32:
                return "SInt32";
            case TYPE_SINT64:
                return "SInt64";
        }
        throw new GeneratorException("Unsupported type: " + type);
    }

    static boolean isFixedWidth(FieldDescriptorProto.Type type) {
        return getFixedWidth(type) > 0;
    }

    static int getFixedWidth(FieldDescriptorProto.Type type) {
        switch (type) {

            // 64 bit
            case TYPE_DOUBLE:
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
                return 8;

            // 32 bit
            case TYPE_FLOAT:
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
                return 4;

            // 8 bit
            case TYPE_BOOL:
                return 1;

            // varint
            default:
                return -1;
        }
    }

    static boolean isPrimitive(FieldDescriptorProto.Type type) {
        switch (type) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_INT64:
            case TYPE_UINT64:
            case TYPE_INT32:
            case TYPE_FIXED64:
            case TYPE_FIXED32:
            case TYPE_BOOL:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SFIXED64:
            case TYPE_SINT32:
            case TYPE_SINT64:
                return true;

            case TYPE_ENUM:
            case TYPE_STRING:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
            case TYPE_BYTES:
                return false;
        }
        throw new GeneratorException("Unsupported type: " + type);
    }

    static String getDefaultValue(FieldDescriptorProto descriptor) {
        final String value = descriptor.getDefaultValue();
        if (value.isEmpty()) return getEmptyDefaultValue(descriptor.getType());

        // Some values need to be special cased to result in valid Java syntax
        switch (descriptor.getType()) {

            case TYPE_DOUBLE:
                switch (value) {
                    case "nan":
                        return "Double.NaN";
                    case "-inf":
                        return "Double.NEGATIVE_INFINITY";
                    case "+inf":
                    case "inf":
                        return "Double.POSITIVE_INFINITY";
                    default:
                        return value + "D";
                }

            case TYPE_FLOAT:
                switch (value) {
                    case "nan":
                        return "Float.NaN";
                    case "-inf":
                        return "Float.NEGATIVE_INFINITY";
                    case "+inf":
                    case "inf":
                        return "Float.POSITIVE_INFINITY";
                    default:
                        return value + "F";
                }

            case TYPE_SFIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
                return value + "L";

            case TYPE_FIXED64: // unsigned in C++
            case TYPE_UINT64:
                return Long.parseUnsignedLong(value) + "L";

            case TYPE_FIXED32: // unsigned in C++
            case TYPE_UINT32:
                return String.valueOf(Integer.parseUnsignedInt(value));

        }

        // Note: Google does some odd things with non-ascii default Strings in order
        // to not need to store UTF-8 or literals with escaped unicode, but I'm really
        // not sure what the problems with either of those options are. Maybe they need
        // to support some archaic Java runtimes that didn't support it?
        return value;

    }

    private static String getEmptyDefaultValue(FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_DOUBLE:
                return "0D";
            case TYPE_FLOAT:
                return "0F";
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return "0L";
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_SINT32:
            case TYPE_INT32:
            case TYPE_UINT32:
                return "0";
            case TYPE_BOOL:
                return "false";
            case TYPE_STRING:
                return "";

            case TYPE_ENUM:
                return "";

            case TYPE_GROUP:
            case TYPE_MESSAGE:
            case TYPE_BYTES:
            default:
                return "";

        }
    }

    /**
     * Hash code for JSON field name lookup. Any changes need to be
     * synchronized between FieldUtil::hash32 and ProtoUtil::hash32.
     */
    static int hash32(String value) {
        return value.hashCode();
    }

}
