package us.hebi.robobuf.compiler;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import lombok.ToString;
import us.hebi.robobuf.compiler.RequestInfo.FileInfo;
import us.hebi.robobuf.compiler.RequestInfo.MessageInfo;
import us.hebi.robobuf.compiler.RequestInfo.TypeInfo;

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

    public static boolean isPrimitive(DescriptorProtos.FieldDescriptorProto.Type type) {
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

            case TYPE_STRING:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
            case TYPE_BYTES:
            case TYPE_ENUM:
                return false;
        }
        throw new GeneratorException("Unsupported type: " + type);
    }

    public TypeName resolveFieldType(RequestInfo.FieldInfo field) {
        switch (field.getDescriptor().getType()) {

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
                return ClassName.get(String.class);
            case TYPE_ENUM:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return resolveClassName(field.getDescriptor().getTypeName());
            case TYPE_BYTES:
                return ArrayTypeName.get(byte[].class);

        }
        throw new GeneratorException("Unsupported type: " + field.getDescriptor());
    }

    public ClassName resolveClassName(String typeId) {
        return checkNotNull(typeMap.get(typeId), "Unable to resolve type id: " + typeId);
    }

    public Map<String, ClassName> getMap() {
        return ImmutableMap.copyOf(typeMap);
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
