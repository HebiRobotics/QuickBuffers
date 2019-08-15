package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;

/**
 * TypeNames of all API classes that can be referenced from generated code
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class RuntimeApi {

    private static final String API_PACKAGE = "us.hebi.robobuf";

    static final ClassName ProtoInput = ClassName.get(API_PACKAGE, "ProtoInput");
    static final ClassName ProtoOutput = ClassName.get(API_PACKAGE, "ProtoOutput");
    static final ClassName InternalUtil = ClassName.get(API_PACKAGE, "InternalUtil");
    static final ClassName Message = ClassName.get(API_PACKAGE, "ProtoMessage");
    static final ClassName MessageFactory = ClassName.get(API_PACKAGE, "MessageFactory");
    static final ClassName StringType = ClassName.get(StringBuilder.class);
    static final ClassName BytesType = ClassName.get(API_PACKAGE, "RepeatedByte");

    static final ClassName RepeatedDouble = ClassName.get(API_PACKAGE, "RepeatedDouble");
    static final ClassName RepeatedFloat = ClassName.get(API_PACKAGE, "RepeatedFloat");
    static final ClassName RepeatedLong = ClassName.get(API_PACKAGE, "RepeatedLong");
    static final ClassName RepeatedInt = ClassName.get(API_PACKAGE, "RepeatedInt");
    static final ClassName RepeatedBoolean = ClassName.get(API_PACKAGE, "RepeatedBoolean");
    static final ClassName RepeatedString = ClassName.get(API_PACKAGE, "RepeatedString");
    static final ClassName RepeatedBytes = ClassName.get(API_PACKAGE, "RepeatedBytes");
    static final ClassName RepeatedMessage = ClassName.get(API_PACKAGE, "RepeatedMessage");

    static ClassName getRepeatedStoreType(FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_DOUBLE:
                return RepeatedDouble;

            case TYPE_FLOAT:
                return RepeatedFloat;

            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return RepeatedLong;

            case TYPE_ENUM:
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_SINT32:
            case TYPE_INT32:
            case TYPE_UINT32:
                return RepeatedInt;

            case TYPE_BOOL:
                return RepeatedBoolean;

            case TYPE_STRING:
                return RepeatedString;

            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return RepeatedMessage;

            case TYPE_BYTES:
                return RepeatedBytes;

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

}
