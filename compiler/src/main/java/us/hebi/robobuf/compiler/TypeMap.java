package us.hebi.robobuf.compiler;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.squareup.javapoet.ClassName;
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
