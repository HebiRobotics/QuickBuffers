package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import lombok.ToString;
import us.hebi.robobuf.compiler.RequestInfo.FileInfo;
import us.hebi.robobuf.compiler.RequestInfo.MessageInfo;
import us.hebi.robobuf.compiler.RequestInfo.TypeInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static us.hebi.robobuf.compiler.Preconditions.*;

/**
 * Maps proto type ids to Java class names
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
@ToString
public class TypeMap {

    static TypeMap fromRequest(CodeGeneratorRequest request) {
        return RequestInfo.withTypeMap(request).getTypeMap();
    }

    static TypeMap empty() {
        return new TypeMap();
    }

    public static int getMinimumPackedSize(Type type) {
        switch (type) {

            case TYPE_DOUBLE:
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
                return 8;


            case TYPE_FLOAT:
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
                return 4;

            // Maybe 2 because of extra fields? Probably doesn't matter
            case TYPE_STRING:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
            case TYPE_BYTES:

                // everything else is varint encoded
            default:
                return 1;
        }
    }

    public static boolean isFixedWidth(Type type) {
        return getFixedWidth(type) > 0;
    }

    public static int getFixedWidth(Type type) {
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

    public static boolean isPrimitive(Type type) {
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

    public TypeName resolveFieldType(FieldDescriptorProto descriptor) {
        switch (descriptor.getType()) {

            case TYPE_DOUBLE:
                return TypeName.DOUBLE;
            case TYPE_FLOAT:
                return TypeName.FLOAT;
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return TypeName.LONG;
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_SINT32:
            case TYPE_INT32:
            case TYPE_UINT32:
                return TypeName.INT;
            case TYPE_BOOL:
                return TypeName.BOOLEAN;
            case TYPE_STRING:
                return RuntimeClasses.STRING_CLASS;
            case TYPE_ENUM:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return resolveClassName(descriptor.getTypeName());
            case TYPE_BYTES:
                return RuntimeClasses.BYTES_CLASS;

        }
        throw new GeneratorException("Unsupported type: " + descriptor);
    }

    public static String getDefaultValue(FieldDescriptorProto descriptor) {
        final String value = descriptor.getDefaultValue();
        if (value.isEmpty())
            return getEmptyDefaultValue(descriptor.getType());

        if (isPrimitive(descriptor.getType())) {

            // Convert special floating point values
            boolean isFloat = (descriptor.getType() == Type.TYPE_FLOAT);
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

    private static String getPrimitiveModifier(FieldDescriptorProto.Type type) {
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


    private int getMaximumExpansionSize(FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_BOOL:
                return 1;

            case TYPE_DOUBLE:
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
                return 8;

            case TYPE_FLOAT:
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
                return 4;

            // may expand to 5 bytes
            case TYPE_SINT32:
            case TYPE_UINT32:
            case TYPE_INT32:
                return 5;

            // may expand to 10 bytes
            case TYPE_SINT64:
            case TYPE_UINT64:
            case TYPE_INT64:
                return 10;

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    public ClassName resolveClassName(String typeId) {
        return checkNotNull(typeMap.get(typeId), "Unable to resolve type id: " + typeId);
    }

    public Map<String, ClassName> getMap() {
        return Collections.unmodifiableMap(typeMap);
    }

    TypeMap buildTypeMap(RequestInfo info) {
        typeMap.clear();
        for (FileInfo file : info.getFiles()) {
            file.getMessageTypes().forEach(this::addType);
            file.getEnumTypes().forEach(this::addType);
        }
        return this;
    }

    private void addType(TypeInfo typeInfo) {
        if (typeMap.values().contains(typeInfo.getTypeName()))
            throw new GeneratorException("Duplicate class name: " + typeInfo.getTypeName());
        if (typeMap.put(typeInfo.getTypeId(), typeInfo.getTypeName()) != null)
            throw new GeneratorException("Duplicate type id: " + typeInfo.getTypeId());

        if (typeInfo instanceof MessageInfo) {
            ((MessageInfo) typeInfo).getNestedTypes().forEach(this::addType);
            ((MessageInfo) typeInfo).getNestedEnums().forEach(this::addType);
        }
    }

    private TypeMap() {
    }

    final Map<String, ClassName> typeMap = new HashMap<>();

}
