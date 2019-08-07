package us.hebi.robobuf.compiler;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.squareup.javapoet.ClassName;
import lombok.ToString;
import us.hebi.robobuf.parser.ParserUtil;

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

    public ClassName getClassName(String typeId) {
        return checkNotNull(typeMap.get(typeId), "Unable to resolve type id: " + typeId);
    }

    public Map<String, ClassName> getMap() {
        return ImmutableMap.copyOf(typeMap);
    }

    public static TypeMap fromRequest(CodeGeneratorRequest request) {
        TypeMap typeMap = new TypeMap();
        Map<String, String> generatorParams = ParserUtil.getGeneratorParameters(request);

        for (FileDescriptorProto protoFile : request.getProtoFileList()) {
            FileParams params = new FileParams(generatorParams, protoFile);
            ClassName outerClass = ClassName.get(params.getJavaPackage(), params.getOuterClassname());
            String typeId = "." + params.getProtoPackage();
            boolean nested = !params.isGenerateMultipleFiles();

            for (DescriptorProto messageType : protoFile.getMessageTypeList()) {
                typeMap.addMessageTypeRecursive(outerClass, nested, typeId, messageType);
            }

            for (EnumDescriptorProto enumType : protoFile.getEnumTypeList()) {
                typeMap.addEnumType(outerClass, nested, typeId, enumType);
            }

        }

        return typeMap;
    }

    private void addMessageTypeRecursive(ClassName outerClass, boolean isNested, String outerTypeId, DescriptorProto messageType) {
        final String name = messageType.getName();
        final String id = outerTypeId + "." + name;
        final ClassName className = isNested ? outerClass.nestedClass(name) : outerClass.peerClass(name);
        addType(id, className);

        for (DescriptorProto nestedMessage : messageType.getNestedTypeList()) {
            addMessageTypeRecursive(className, true, id, nestedMessage);
        }

        for (EnumDescriptorProto nestedEnum : messageType.getEnumTypeList()) {
            addEnumType(className, true, id, nestedEnum);
        }

    }

    private void addEnumType(ClassName outerClass, boolean isNested, String outerTypeId, EnumDescriptorProto enumType) {
        final String name = enumType.getName();
        final String id = outerTypeId + "." + name;
        final ClassName className = isNested ? outerClass.nestedClass(name) : outerClass.peerClass(name);
        addType(id, className);
    }

    private void addType(String typeId, ClassName className) {
        if (typeMap.values().contains(className))
            throw new GeneratorException("Duplicate class name: " + className);
        if (typeMap.put(typeId, className) != null)
            throw new GeneratorException("Duplicate type id: " + typeId);
    }

    private TypeMap() {
    }

    final Map<String, ClassName> typeMap = new HashMap<>();

}
