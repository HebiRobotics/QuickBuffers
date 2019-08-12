package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class RuntimeClasses {

    private static final String API_PACKAGE = "us.hebi.robobuf";
    public static final ClassName PROTO_SOURCE = ClassName.get(API_PACKAGE, "CodedInputByteBufferNano");
    public static final ClassName PROTO_DEST = ClassName.get(API_PACKAGE, "CodedOutputByteBufferNano");
    public static final ClassName BASE_MESSAGE = ClassName.get(API_PACKAGE, "MessageNano");
    public static final ClassName STRING_CLASS = ClassName.get(CharSequence.class);
    public static final ClassName STRING_STORAGE_CLASS = ClassName.get(StringBuilder.class);
    public static final ClassName UNKNOWN_FIELD_PARSE_CLASS = ClassName.get(API_PACKAGE, "WireFormatNano");
    public static final ClassName ROBO_UTIL = ClassName.get(API_PACKAGE, "RoboUtil");
    public static final ClassName WIRE_FORMAT = ClassName.get(API_PACKAGE, "WireFormatNano");

    private static final int WIRETYPE_VARINT = 0;
    private static final int WIRETYPE_FIXED64 = 1;
    private static final int WIRETYPE_LENGTH_DELIMITED = 2;
    private static final int WIRETYPE_START_GROUP = 3;
    private static final int WIRETYPE_END_GROUP = 4;
    private static final int WIRETYPE_FIXED32 = 5;

    private static final int TAG_TYPE_BITS = 3;
    private static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

    static ClassName getArrayStoreType(FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_DOUBLE:
                return ClassName.get(API_PACKAGE, "RepeatedDouble");

            case TYPE_FLOAT:
                return ClassName.get(API_PACKAGE, "RepeatedFloat");

            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return ClassName.get(API_PACKAGE, "RepeatedLong");

            case TYPE_ENUM:
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_SINT32:
            case TYPE_INT32:
            case TYPE_UINT32:
                return ClassName.get(API_PACKAGE, "RepeatedInt");

            case TYPE_BOOL:
                return ClassName.get(API_PACKAGE, "RepeatedBoolean");

            case TYPE_STRING:
                return ClassName.get(API_PACKAGE, "RepeatedString");

            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return ClassName.get(API_PACKAGE, "RepeatedMessage");

            case TYPE_BYTES:
                return ClassName.get(API_PACKAGE, "RepeatedBytes");

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    public static int makeTag(FieldDescriptorProto descriptor) {
        return descriptor.getNumber() << TAG_TYPE_BITS | getWireType(descriptor.getType());
    }

    public static int makePackedTag(FieldDescriptorProto descriptor) {
        return descriptor.getNumber() << 3 | WIRETYPE_LENGTH_DELIMITED;
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
    public static String getCapitalizedType(FieldDescriptorProto.Type type) {
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

}
