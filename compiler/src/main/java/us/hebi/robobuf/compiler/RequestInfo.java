package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import us.hebi.robobuf.parser.ParserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static us.hebi.robobuf.compiler.Preconditions.*;

/**
 * Meta info that wraps the information in descriptors in a format that is easier to work with
 *
 * @author Florian Enner
 * @since 06 Aug 2019
 */
@Getter
@EqualsAndHashCode
@ToString
public class RequestInfo {

    public static RequestInfo withoutTypeMap(CodeGeneratorRequest request) {
        return new RequestInfo(request);
    }

    public static RequestInfo withTypeMap(CodeGeneratorRequest request) {
        RequestInfo info = new RequestInfo(request);
        info.typeMap.buildTypeMap(info);
        return info;
    }

    private RequestInfo(CodeGeneratorRequest descriptor) {
        this.descriptor = descriptor;
        this.generatorParameters = ParserUtil.parseGeneratorParameters(descriptor.getParameter());
        this.files = descriptor.getProtoFileList().stream()
                .map(desc -> new FileInfo(this, desc))
                .collect(Collectors.toList());
    }

    public ClassName resolveClassName(String typeId) {
        return checkNotNull(typeMap.resolveClassName(typeId), "Unable to resolve type id: " + typeId);
    }

    /**
     * replacePackage=pattern=replacement
     *
     * @param javaPackage
     * @return
     */
    public String applyJavaPackageReplace(String javaPackage) {
        String replaceOption = generatorParameters.get("replacePackage");
        if (replaceOption == null)
            return javaPackage;

        String[] parts = replaceOption.split("=");
        if (parts.length != 2)
            throw new GeneratorException("'replacePackage' expects 'pattern=replacement'. Found: '" + replaceOption + "'");

        return javaPackage.replaceAll(parts[0], parts[1]);
    }

    public String getIndentString() {
        String indent = generatorParameters.getOrDefault("indent", "2");
        switch (indent) {
            case "8":
                return "        ";
            case "4":
                return "    ";
            case "2":
                return "  ";
            case "tab":
                return "\t";
        }
        throw new GeneratorException("Expected 2,4,8,tab. Found: " + indent);
    }

    public boolean shouldEnumUseArrayLookup(int highestNumber) {
        return highestNumber < 50; // parameter?
    }

    private final CodeGeneratorRequest descriptor;
    private final Map<String, String> generatorParameters;
    private final List<FileInfo> files;
    private final TypeMap typeMap = TypeMap.empty();

    @Value
    public static class FileInfo {

        FileInfo(RequestInfo parentRequest, FileDescriptorProto descriptor) {
            this.parentRequest = parentRequest;
            this.descriptor = descriptor;

            fileName = descriptor.getName();
            protoPackage = NameResolver.getProtoPackage(descriptor);

            javaPackage = getParentRequest().applyJavaPackageReplace(
                    NameResolver.getJavaPackage(descriptor));

            outerClassName = ClassName.get(javaPackage, NameResolver.getJavaOuterClassname(descriptor));

            outputDirectory = javaPackage.isEmpty() ? "" : javaPackage.replaceAll("\\.", "/") + "/";

            DescriptorProtos.FileOptions options = descriptor.getOptions();
            generateMultipleFiles = options.hasJavaMultipleFiles() && options.getJavaMultipleFiles();
            deprecated = options.hasDeprecated() && options.getDeprecated();

            baseTypeId = "." + protoPackage;

            messageTypes = descriptor.getMessageTypeList().stream()
                    .map(desc -> new MessageInfo(this, baseTypeId, outerClassName, !generateMultipleFiles, desc))
                    .collect(Collectors.toList());

            enumTypes = descriptor.getEnumTypeList().stream()
                    .map(desc -> new EnumInfo(this, baseTypeId, outerClassName, !generateMultipleFiles, desc))
                    .collect(Collectors.toList());

        }

        private final RequestInfo parentRequest;
        private final FileDescriptorProto descriptor;
        private final String baseTypeId;
        private final String fileName;
        private final boolean generateMultipleFiles;

        private final ClassName outerClassName;
        private final String protoPackage;
        private final String javaPackage;
        private final String outputDirectory;

        private final boolean deprecated;

        private final List<MessageInfo> messageTypes;
        private final List<EnumInfo> enumTypes;

    }

    @Getter
    @EqualsAndHashCode
    public static abstract class TypeInfo {

        protected TypeInfo(FileInfo parentFile, String parentTypeId, ClassName parentType, boolean isNested, String name) {
            this.parentFile = parentFile;
            this.typeName = isNested ? parentType.nestedClass(name) : parentType.peerClass(name);
            this.typeId = parentTypeId + "." + name;
            this.isNested = isNested;
        }

        private final FileInfo parentFile;
        protected final boolean isNested;
        protected final String typeId;
        protected final ClassName typeName;

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class MessageInfo extends TypeInfo {

        MessageInfo(FileInfo parentFile, String parentTypeId, ClassName parentType, boolean isNested, DescriptorProtos.DescriptorProto descriptor) {
            super(parentFile, parentTypeId, parentType, isNested, descriptor.getName());
            this.descriptor = descriptor;
            this.fieldCount = descriptor.getFieldCount();

            int fieldIndex = 0;
            for (FieldDescriptorProto desc : descriptor.getFieldList()) {
                fields.add(new FieldInfo(parentFile, typeName, desc, fieldIndex++));
            }

            nestedTypes = descriptor.getNestedTypeList().stream()
                    .map(desc -> new MessageInfo(parentFile, typeId, typeName, true, desc))
                    .collect(Collectors.toList());

            nestedEnums = descriptor.getEnumTypeList().stream()
                    .map(desc -> new EnumInfo(parentFile, typeId, typeName, true, desc))
                    .collect(Collectors.toList());

        }

        private final DescriptorProtos.DescriptorProto descriptor;
        private final int fieldCount;
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<MessageInfo> nestedTypes;
        private final List<EnumInfo> nestedEnums;

    }

    @Value
    public static class FieldInfo {

        FieldInfo(FileInfo parentFile, ClassName parentType, FieldDescriptorProto descriptor, int fieldIndex) {
            this.parentFile = parentFile;
            this.parentType = parentType;
            this.descriptor = descriptor;
            this.fieldIndex = fieldIndex;

            hasBit = BitField.hasBit(fieldIndex);
            setBit = BitField.setBit(fieldIndex);
            clearBit = BitField.clearBit(fieldIndex);
            if (isGroup()) {
                // name is all lowercase, so convert the type name instead (e.g. ".package.OptionalGroup")
                String name = descriptor.getTypeName();
                int packageEndIndex = name.lastIndexOf('.');
                upperName = packageEndIndex > 0 ? name.substring(packageEndIndex + 1, name.length()) : name;
            } else {
                upperName = NameUtil.toUpperCamel(descriptor.getName());
            }
            lowerName = Character.toLowerCase(upperName.charAt(0)) + upperName.substring(1);
            hazzerName = "has" + upperName;
            setterName = "set" + upperName;
            getterName = "get" + upperName;
            mutableGetterName = "getMutable" + upperName;
            adderName = "add" + upperName;
            clearName = "clear" + upperName;
            isPrimitive = TypeMap.isPrimitive(descriptor.getType());
            tag = RuntimeClasses.makeTag(descriptor);
            bytesPerTag = RuntimeClasses.computeRawVarint32Size(tag);
            packedTag = RuntimeClasses.makePackedTag(descriptor);
            number = descriptor.getNumber();
            fieldName = NameUtil.filterKeyword(lowerName);
            final String defValue = TypeMap.getDefaultValue(descriptor);
            defaultValue = isEnum() ? NameUtil.filterKeyword(defValue) : defValue;
            repeatedStoreType = RuntimeClasses.getArrayStoreType(descriptor.getType());

        }

        public TypeName getRepeatedStoreType() {
            if (isGroup() || isMessage()) {
                return ParameterizedTypeName.get(repeatedStoreType, getTypeName());
            }
            return repeatedStoreType;
        }

        public String getJavadoc() {
            return descriptor.toBuilder().clearJsonName().toString();
        }

        public boolean isFixedWidth() {
            return TypeMap.isFixedWidth(descriptor.getType());
        }

        public int getFixedWidth() {
            checkState(isFixedWidth(), "not a fixed width type");
            return TypeMap.getFixedWidth(descriptor.getType());
        }

        public boolean isMessageOrGroup() {
            return isMessage() || isGroup();
        }

        public String getDefaultFieldName() {
            return "_default" + getUpperName();
        }

        public TypeName getInputParameterType() {
            switch (descriptor.getType()) {
                case TYPE_STRING:
                    return TypeName.get(CharSequence.class);
                case TYPE_BYTES:
                    return isRepeated() ? ArrayTypeName.of(TypeName.BYTE) : TypeName.BYTE;
            }
            return getTypeName();
        }

        public boolean isGroup() {
            return descriptor.getType() == FieldDescriptorProto.Type.TYPE_GROUP;
        }

        public boolean isMessage() {
            return descriptor.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE;
        }

        public boolean isString() {
            return descriptor.getType() == FieldDescriptorProto.Type.TYPE_STRING;
        }

        public boolean isBytes() {
            return descriptor.getType() == FieldDescriptorProto.Type.TYPE_BYTES;
        }

        public boolean isEnum() {
            return descriptor.getType() == FieldDescriptorProto.Type.TYPE_ENUM;
        }

        public boolean isRequired() {
            return descriptor.getLabel() == FieldDescriptorProto.Label.LABEL_REQUIRED;
        }

        public boolean isOptional() {
            return descriptor.getLabel() == FieldDescriptorProto.Label.LABEL_OPTIONAL;
        }

        public boolean isRepeated() {
            return descriptor.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED;
        }

        public boolean isPacked() {
            return isPackable() && descriptor.getOptions().hasPacked() && descriptor.getOptions().getPacked();
        }

        public boolean isPackable() {
            if (!isRepeated())
                return false;

            switch (descriptor.getType()) {
                case TYPE_STRING:
                case TYPE_GROUP:
                case TYPE_MESSAGE:
                case TYPE_BYTES:
                    return false;
                default:
                    return true;
            }
        }

        public boolean hasDefaultValue() {
            return !defaultValue.isEmpty();
        }

        public boolean isDeprecated() {
            return descriptor.getOptions().hasDeprecated() && descriptor.getOptions().getDeprecated();
        }

        public TypeName getTypeName() {
            // Lazy because type map is not constructed at creation time
            return getParentFile().getParentRequest().getTypeMap().resolveFieldType(descriptor);
        }

        public TypeName getStoreType() {
            if (isRepeated())
                return getRepeatedStoreType();
            if (isString())
                return RuntimeClasses.STRING_CLASS;
            if (isEnum())
                return TypeName.INT;
            return getTypeName();
        }

        private final FileInfo parentFile;
        private final ClassName parentType;
        private final ClassName repeatedStoreType;
        private final FieldDescriptorProto descriptor;
        private final int fieldIndex;
        private final String hasBit;
        private final String setBit;
        private final String clearBit;
        private final boolean isPrimitive;
        String fieldName;
        String lowerName;
        String upperName;
        String hazzerName;
        String setterName;
        String getterName;
        String mutableGetterName;
        String adderName;
        String clearName;
        String defaultValue;
        int tag;
        int bytesPerTag;
        int packedTag;
        int number;

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class EnumInfo extends TypeInfo {

        EnumInfo(FileInfo parentFile, String parentTypeId, ClassName parentType, boolean isNested, EnumDescriptorProto descriptor) {
            super(parentFile, parentTypeId, parentType, isNested, descriptor.getName());
            this.descriptor = descriptor;
            this.highestNumber = descriptor.getValueList().stream()
                    .mapToInt(DescriptorProtos.EnumValueDescriptorProto::getNumber)
                    .max().orElseGet(() -> 0);
            this.usingArrayLookup = parentFile.getParentRequest().shouldEnumUseArrayLookup(highestNumber);
        }

        public List<DescriptorProtos.EnumValueDescriptorProto> getValues() {
            return descriptor.getValueList();
        }

        private final EnumDescriptorProto descriptor;
        private final int highestNumber;
        private final boolean usingArrayLookup;

    }

}
