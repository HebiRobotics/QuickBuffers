package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class ProtoUtil {

    static int makeTag(DescriptorProtos.FieldDescriptorProto descriptor) {
        return descriptor.getNumber() << TAG_TYPE_BITS | getWireType(descriptor.getType());
    }

    static int makePackedTag(DescriptorProtos.FieldDescriptorProto descriptor) {
        return descriptor.getNumber() << 3 | WIRETYPE_LENGTH_DELIMITED;
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

    private static int getWireType(DescriptorProtos.FieldDescriptorProto.Type type) {
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
    static String getCapitalizedType(DescriptorProtos.FieldDescriptorProto.Type type) {
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

    private static final int WIRETYPE_VARINT = 0;
    private static final int WIRETYPE_FIXED64 = 1;
    private static final int WIRETYPE_LENGTH_DELIMITED = 2;
    private static final int WIRETYPE_START_GROUP = 3;
    private static final int WIRETYPE_END_GROUP = 4;
    private static final int WIRETYPE_FIXED32 = 5;
    private static final int TAG_TYPE_BITS = 3;
    private static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

    static boolean isFixedWidth(DescriptorProtos.FieldDescriptorProto.Type type) {
        return getFixedWidth(type) > 0;
    }

    static int getFixedWidth(DescriptorProtos.FieldDescriptorProto.Type type) {
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

    static boolean isPrimitive(DescriptorProtos.FieldDescriptorProto.Type type) {
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

    static String getDefaultValue(DescriptorProtos.FieldDescriptorProto descriptor) {
        final String value = descriptor.getDefaultValue();
        if (value.isEmpty())
            return getEmptyDefaultValue(descriptor.getType());

        if (isPrimitive(descriptor.getType())) {

            // Convert special floating point values
            boolean isFloat = (descriptor.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
            String constantClass = isFloat ? "Float" : "Double";
            switch (value) {
                case "nan":
                    return constantClass + ".NaN";
                case "-inf":
                    return constantClass + ".NEGATIVE_INFINITY";
                case "+inf":
                case "inf":
                    return constantClass + ".POSITIVE_INFINITY";
            }

            // Add modifiers
            String modifier = getPrimitiveModifier(descriptor.getType());
            if (modifier.isEmpty())
                return value;

            char modifierChar = Character.toUpperCase(modifier.charAt(0));
            char lastChar = Character.toUpperCase(value.charAt(value.length() - 1));

            if (lastChar == modifierChar)
                return value;
            return value + modifierChar;

        }

        // Note: Google does some odd things with non-ascii default Strings in order
        // to not need to store UTF-8 or literals with escaped unicode, but I'm really
        // not sure what the problems with either of those options are. Maybe they need
        // to support some archaic Java runtimes that didn't support it?
        return value;

    }

    private static String getEmptyDefaultValue(DescriptorProtos.FieldDescriptorProto.Type type) {
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

    private static String getPrimitiveModifier(DescriptorProtos.FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_DOUBLE:
                return "D";
            case TYPE_FLOAT:
                return "F";
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return "L";
            default:
                return "";

        }
    }

}
