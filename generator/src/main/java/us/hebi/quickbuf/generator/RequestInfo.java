/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.quickbuf.generator;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.squareup.javapoet.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import us.hebi.quickbuf.parser.ParserUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static us.hebi.quickbuf.generator.Preconditions.*;

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

    public static RequestInfo withTypeRegistry(CodeGeneratorRequest request) {
        RequestInfo info = new RequestInfo(request);
        info.typeRegistry.registerContainedTypes(info);
        return info;
    }

    private RequestInfo(CodeGeneratorRequest descriptor) {
        this.descriptor = descriptor;
        this.generatorParameters = ParserUtil.parseGeneratorParameters(descriptor.getParameter());
        this.files = descriptor.getProtoFileList().stream()
                .map(desc -> new FileInfo(this, desc))
                .collect(Collectors.toList());
    }

    /**
     * replace_package=pattern=replacement
     *
     * @param javaPackage
     * @return
     */
    public String applyJavaPackageReplace(String javaPackage) {
        String replaceOption = generatorParameters.get("replace_package");
        if (replaceOption == null)
            return javaPackage;

        String[] parts = replaceOption.split("=");
        if (parts.length != 2)
            throw new GeneratorException("'replace_package' expects 'pattern=replacement'. Found: '" + replaceOption + "'");

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

    enum ExpectedIncomingOrder {
        Quickbuf, // parsing messages from Quickbuf
        AscendingNumber, // parsing messages from official protobuf bindings
        None; // parsing messages from unknown sources

        @Override
        public String toString() {
            switch (this) {
                case Quickbuf:
                    return "QuickBuffers";
                case AscendingNumber:
                    return "Sorted by Field Numbers";
                default:
                    return name();
            }
        }
    }

    public ExpectedIncomingOrder getExpectedIncomingOrder() {
        String order = generatorParameters.getOrDefault("input_order", "quickbuf");
        switch (order.toLowerCase()) {
            case "quickbuf":
                return ExpectedIncomingOrder.Quickbuf;
            case "number":
                return ExpectedIncomingOrder.AscendingNumber;
            case "random":
            case "none":
                return ExpectedIncomingOrder.None;
        }
        throw new GeneratorException("Expected input_order quickbuf,number,random. Found: " + order);
    }

    public boolean shouldEnumUseArrayLookup(int highestNumber) {
        return highestNumber < 50; // parameter?
    }

    public boolean getStoreUnknownFields() {
        return Boolean.parseBoolean(generatorParameters.getOrDefault("store_unknown_fields", "false"));
    }

    public boolean getJsonUseProtoName() {
        return Boolean.parseBoolean(generatorParameters.getOrDefault("json_use_proto_name", "false"));
    }

    private final CodeGeneratorRequest descriptor;
    private final Map<String, String> generatorParameters;
    private final List<FileInfo> files;
    private final TypeRegistry typeRegistry = TypeRegistry.empty();

    @Value
    public static class FileInfo {

        FileInfo(RequestInfo parentRequest, FileDescriptorProto descriptor) {
            this.parentRequest = parentRequest;
            this.descriptor = descriptor;

            fileName = descriptor.getName();
            protoPackage = NamingUtil.getProtoPackage(descriptor);

            javaPackage = getParentRequest().applyJavaPackageReplace(
                    NamingUtil.getJavaPackage(descriptor));

            outerClassName = ClassName.get(javaPackage, NamingUtil.getJavaOuterClassname(descriptor));

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
            this.jsonKeysClass = this.typeName.nestedClass("JsonKeys");
            this.typeId = parentTypeId + "." + name;
            this.isNested = isNested;
        }

        private final FileInfo parentFile;
        protected final boolean isNested;
        protected final String typeId;
        protected final ClassName typeName;
        protected final ClassName jsonKeysClass;

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class MessageInfo extends TypeInfo {

        MessageInfo(FileInfo parentFile, String parentTypeId, ClassName parentType, boolean isNested, DescriptorProtos.DescriptorProto descriptor) {
            super(parentFile, parentTypeId, parentType, isNested, descriptor.getName());
            this.descriptor = descriptor;
            this.fieldCount = descriptor.getFieldCount();
            this.storeUnknownFields = parentFile.getParentRequest().getStoreUnknownFields();

            int fieldIndex = 0;
            for (FieldDescriptorProto desc : descriptor.getFieldList().stream()
                    .sorted(FieldUtil.MemoryLayoutSorter)
                    .collect(Collectors.toList())) {
                fields.add(new FieldInfo(parentFile, this, typeName, desc, fieldIndex++));
            }

            nestedTypes = descriptor.getNestedTypeList().stream()
                    .map(desc -> new MessageInfo(parentFile, typeId, typeName, true, desc))
                    .collect(Collectors.toList());

            nestedEnums = descriptor.getEnumTypeList().stream()
                    .map(desc -> new EnumInfo(parentFile, typeId, typeName, true, desc))
                    .collect(Collectors.toList());

            int oneOfIndex = 0;
            for (OneofDescriptorProto desc : descriptor.getOneofDeclList()) {
                oneOfs.add(new OneOfInfo(parentFile, this, typeName, desc, oneOfIndex++));
            }

            expectedIncomingOrder = getParentFile().getParentRequest().getExpectedIncomingOrder();
            numBitFields = BitField.getNumberOfFields(fields.size());

        }

        private final DescriptorProtos.DescriptorProto descriptor;
        private final int fieldCount;
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<MessageInfo> nestedTypes;
        private final List<EnumInfo> nestedEnums;
        private final List<OneOfInfo> oneOfs = new ArrayList<>();
        private final ExpectedIncomingOrder expectedIncomingOrder;
        private final boolean storeUnknownFields;
        private final int numBitFields;

    }

    @Value
    public static class FieldInfo {

        FieldInfo(FileInfo parentFile, MessageInfo parentTypeInfo, ClassName parentType, FieldDescriptorProto descriptor, int fieldIndex) {
            this.parentFile = parentFile;
            this.parentTypeInfo = parentTypeInfo;
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
                upperName = NamingUtil.toUpperCamel(descriptor.getName());
            }
            lowerName = Character.toLowerCase(upperName.charAt(0)) + upperName.substring(1);
            hazzerName = "has" + upperName;
            setterName = "set" + upperName;
            getterName = "get" + upperName;
            mutableGetterName = "getMutable" + upperName;
            adderName = "add" + upperName;
            clearName = "clear" + upperName;
            isPrimitive = FieldUtil.isPrimitive(descriptor.getType());
            tag = FieldUtil.makeTag(descriptor);
            bytesPerTag = FieldUtil.computeRawVarint32Size(tag);
            packedTag = FieldUtil.makePackedTag(descriptor);
            number = descriptor.getNumber();
            fieldName = NamingUtil.filterKeyword(lowerName);
            final String defValue = FieldUtil.getDefaultValue(descriptor);
            defaultValue = isEnum() ? NamingUtil.filterKeyword(defValue) : defValue;
            repeatedStoreType = RuntimeClasses.getRepeatedStoreType(descriptor.getType());
            methodAnnotations = isDeprecated() ?
                    Collections.singletonList(AnnotationSpec.builder(Deprecated.class).build()) :
                    Collections.emptyList();
        }

        public TypeName getRepeatedStoreType() {
            if (isGroup() || isMessage()) {
                return ParameterizedTypeName.get(repeatedStoreType, getTypeName());
            } else if (isEnum()) {
                return ParameterizedTypeName.get(repeatedStoreType, getTypeName());
            }
            return repeatedStoreType;
        }

        public String getJavadoc() {
            return FieldUtil.getProtoDefinitionLine(descriptor) + "\n";
        }

        public boolean isFixedWidth() {
            return FieldUtil.isFixedWidth(descriptor.getType());
        }

        public int getFixedWidth() {
            checkState(isFixedWidth(), "not a fixed width type");
            return FieldUtil.getFixedWidth(descriptor.getType());
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
            // Lazy because type registry is not constructed at creation time
            return getParentFile().getParentRequest().getTypeRegistry().resolveJavaTypeFromProto(descriptor);
        }

        public TypeName getStoreType() {
            if (isRepeated())
                return getRepeatedStoreType();
            if (isString())
                return RuntimeClasses.StringType;
            if (isEnum())
                return TypeName.INT;
            return getTypeName();
        }

        // Used for serialization
        public String getPrimaryJsonName() {
            return parentFile.getParentRequest().getJsonUseProtoName() ?
                    descriptor.getName() : descriptor.getJsonName();
        }

        // Parsing should support both
        public String getSecondaryJsonName() {
            return !parentFile.getParentRequest().getJsonUseProtoName() ?
                    descriptor.getName() : descriptor.getJsonName();
        }

        public boolean hasOtherOneOfFields() {
            return descriptor.hasOneofIndex()
                    && getParentTypeInfo()
                    .getOneOfs()
                    .get(descriptor.getOneofIndex())
                    .getFields().size() > 1;
        }

        public List<FieldInfo> getOtherOneOfFields() {
            if (!descriptor.hasOneofIndex())
                return Collections.emptyList();

            final int index = descriptor.getOneofIndex();
            return parentTypeInfo.getFields().stream()
                    .filter(field -> field.getDescriptor().hasOneofIndex())
                    .filter(field -> field.getDescriptor().getOneofIndex() == index)
                    .filter(field -> field != this)
                    .collect(Collectors.toList());
        }

        private final FileInfo parentFile;
        private final MessageInfo parentTypeInfo;
        private final ClassName parentType;
        private final ClassName repeatedStoreType;
        private final FieldDescriptorProto descriptor;
        private final int fieldIndex;
        private final String hasBit;
        private final String setBit;
        private final String clearBit;
        private final boolean isPrimitive;
        private final List<AnnotationSpec> methodAnnotations;
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

    @Value
    public static class OneOfInfo {
        OneOfInfo(FileInfo parentFile, MessageInfo parentTypeInfo, ClassName parentType, OneofDescriptorProto descriptor, int oneOfIndex) {
            this.parentFile = parentFile;
            this.parentTypeInfo = parentTypeInfo;
            this.parentType = parentType;
            this.descriptor = descriptor;
            this.oneOfIndex = oneOfIndex;

            upperName = NamingUtil.toUpperCamel(descriptor.getName());
            hazzerName = "has" + upperName;
            clearName = "clear" + upperName;

        }

        public List<FieldInfo> getFields() {
            return parentTypeInfo.getFields().stream()
                    .filter(field -> field.getDescriptor().hasOneofIndex())
                    .filter(field -> field.getDescriptor().getOneofIndex() == oneOfIndex)
                    .collect(Collectors.toList());
        }

        private final FileInfo parentFile;
        private final MessageInfo parentTypeInfo;
        private final ClassName parentType;
        private final OneofDescriptorProto descriptor;
        private final int oneOfIndex;
        private final String upperName;
        private final String hazzerName;
        private final String clearName;

    }

}
