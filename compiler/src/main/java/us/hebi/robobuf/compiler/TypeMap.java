package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
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
class TypeMap {

    static TypeMap empty() {
        return new TypeMap();
    }

    TypeName resolveFieldType(FieldDescriptorProto descriptor) {
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
                return RuntimeApi.StringType;
            case TYPE_ENUM:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return resolveClassName(descriptor.getTypeName());
            case TYPE_BYTES:
                return RuntimeApi.BytesType;

        }
        throw new GeneratorException("Unsupported type: " + descriptor);
    }

    ClassName resolveClassName(String typeId) {
        return checkNotNull(typeMap.get(typeId), "Unable to resolve type id: " + typeId);
    }

    void buildTypeMap(RequestInfo info) {
        typeMap.clear();
        for (FileInfo file : info.getFiles()) {
            file.getMessageTypes().forEach(this::addType);
            file.getEnumTypes().forEach(this::addType);
        }
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
