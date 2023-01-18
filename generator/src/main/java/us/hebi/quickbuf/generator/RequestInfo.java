/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import us.hebi.quickbuf.generator.PluginOptions.AllocationStrategy;
import us.hebi.quickbuf.generator.PluginOptions.ExpectedIncomingOrder;

import java.util.*;
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
        this.pluginOptions = new PluginOptions(descriptor);
        this.files = descriptor.getProtoFileList().stream()
                .map(desc -> new FileInfo(this, desc))
                .collect(Collectors.toList());
    }

    public boolean shouldEnumUseArrayLookup(int highestNumber) {
        return highestNumber < 50; // parameter?
    }

    private final CodeGeneratorRequest descriptor;
    private final PluginOptions pluginOptions;
    private final List<FileInfo> files;
    private final TypeRegistry typeRegistry = TypeRegistry.empty();

    @Value
    public static class FileInfo {

        FileInfo(RequestInfo parentRequest, FileDescriptorProto descriptor) {
            this.parentRequest = parentRequest;
            this.descriptor = descriptor;

            fileName = descriptor.getName();
            protoPackage = NamingUtil.getProtoPackage(descriptor);

            javaPackage = getParentRequest()
                    .getPluginOptions()
                    .getReplacePackageFunction()
                    .apply(NamingUtil.getJavaPackage(descriptor));

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
            this.fieldNamesClass = this.typeName.nestedClass("FieldNames");
            this.typeId = parentTypeId + "." + name;
            this.isNested = isNested;
        }

        private final FileInfo parentFile;
        protected final boolean isNested;
        protected final String typeId;
        protected final ClassName typeName;
        protected final ClassName fieldNamesClass;

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class MessageInfo extends TypeInfo {

        MessageInfo(FileInfo parentFile, String parentTypeId, ClassName parentType, boolean isNested, DescriptorProtos.DescriptorProto descriptor) {
            super(parentFile, parentTypeId, parentType, isNested, descriptor.getName());
            this.descriptor = descriptor;
            this.fieldCount = descriptor.getFieldCount();

            PluginOptions options = parentFile.getParentRequest().getPluginOptions();
            this.expectedIncomingOrder = options.getExpectedIncomingOrder();
            this.storeUnknownFieldsEnabled = options.isStoreUnknownFieldsEnabled();
            this.enforceHasChecksEnabled = options.isEnforceHasChecksEnabled();

            // Sort fields by serialization order such that they are accessed in a
            // sequential access pattern.
            List<FieldDescriptorProto> sortedFields = descriptor.getFieldList().stream()
                    .sorted(FieldUtil.MemoryLayoutSorter)
                    .collect(Collectors.toList());

            // Build bitfield index map. In the case of OneOf fields we want them grouped
            // together so that we can check all has states in as few bitfield comparisons
            // as possible. If there are no OneOf fields, the order will match the field
            // order.
            int bitIndex = 0;
            Map<FieldDescriptorProto, Integer> bitIndices = new HashMap<>();
            for (FieldDescriptorProto desc : sortedFields.stream()
                    .sorted(FieldUtil.GroupOneOfAndRequiredBits)
                    .collect(Collectors.toList())) {
                bitIndices.put(desc, bitIndex++);
            }

            // Build map
            for (FieldDescriptorProto desc : sortedFields) {
                fields.add(new FieldInfo(parentFile, this, typeName, desc, bitIndices.get(desc)));
            }

            nestedTypes = descriptor.getNestedTypeList().stream()
                    .map(desc -> new MessageInfo(parentFile, typeId, typeName, true, desc))
                    .collect(Collectors.toList());

            nestedEnums = descriptor.getEnumTypeList().stream()
                    .map(desc -> new EnumInfo(parentFile, typeId, typeName, true, desc))
                    .collect(Collectors.toList());

            int oneOfCount = descriptor.getOneofDeclCount();
            Set<Integer> syntheticIndices = getSyntheticOneOfIndices();
            for (int i = 0; i < oneOfCount; i++) {
                oneOfs.add(new OneOfInfo(parentFile, this, typeName,
                        descriptor.getOneofDecl(i), syntheticIndices.contains(i), i));
            }

            numBitFields = BitField.getNumberOfFields(fields.size());

        }

        private Set<Integer> getSyntheticOneOfIndices() {
            // Filter synthetic OneOfs for single-fields (proto3 explicit optionals)
            // see https://github.com/protocolbuffers/protobuf/blob/d36a64116f19ce59acf3af49e66cadef4c2fb2df/src/google/protobuf/descriptor.proto#L219-L240
            // TODO: implement https://github.com/protocolbuffers/protobuf/blob/f75fd051d68136ce366c464cea4f3074158cd141/docs/implementing_proto3_presence.md#api-changes
            Set<Integer> syntheticIndices = new HashSet<>();
            descriptor.getFieldList().stream()
                    .filter(FieldDescriptorProto::hasProto3Optional)
                    .filter(FieldDescriptorProto::getProto3Optional)
                    .filter(FieldDescriptorProto::hasOneofIndex)
                    .mapToInt(FieldDescriptorProto::getOneofIndex)
                    .forEach(syntheticIndices::add);
            return syntheticIndices;
        }

        public boolean hasRequiredFieldsInHierarchy() {
            return getParentFile().getParentRequest().getTypeRegistry().hasRequiredFieldsInHierarchy(typeName);
        }

        private final DescriptorProtos.DescriptorProto descriptor;
        private final int fieldCount;
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<MessageInfo> nestedTypes;
        private final List<EnumInfo> nestedEnums;
        private final List<OneOfInfo> oneOfs = new ArrayList<>();
        private final ExpectedIncomingOrder expectedIncomingOrder;
        private final boolean storeUnknownFieldsEnabled;
        private final int numBitFields;
        private final boolean enforceHasChecksEnabled;

    }

    @Value
    public static class FieldInfo {

        FieldInfo(FileInfo parentFile, MessageInfo parentTypeInfo, ClassName parentType, FieldDescriptorProto descriptor, int bitIndex) {
            this.parentFile = parentFile;
            this.parentTypeInfo = parentTypeInfo;
            this.parentType = parentType;
            this.descriptor = descriptor;
            this.bitIndex = bitIndex;
            this.storeUnknownFieldsEnabled = parentTypeInfo.isStoreUnknownFieldsEnabled();

            hasBit = BitField.hasBit(bitIndex);
            setBit = BitField.setBit(bitIndex);
            clearBit = BitField.clearBit(bitIndex);
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
            tryGetName = "tryGet" + upperName;
            mutableGetterName = "getMutable" + upperName;
            adderName = "add" + upperName;
            clearName = "clear" + upperName;
            isPrimitive = FieldUtil.isPrimitive(descriptor.getType());
            tag = FieldUtil.makeTag(descriptor);
            bytesPerTag = FieldUtil.computeRawVarint32Size(tag) +
                    (!isGroup() ? 0 : FieldUtil.computeRawVarint32Size(getEndGroupTag()));
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

        public boolean isLazyAllocationEnabled() {
            // currently only supported for nested types
            return getPluginOptions().getAllocationStrategy() == AllocationStrategy.Lazy
                    && !isRequired()
                    && !isRepeated()
                    && isMessageOrGroup();
        }

        public boolean isEnforceHasCheckEnabled() {
            return getPluginOptions().isEnforceHasChecksEnabled();
        }

        public boolean isTryGetAccessorEnabled() {
            return getPluginOptions().isTryGetAccessorsEnabled();
        }

        public boolean isPresenceEnabled() {
            // Checks whether field presence is enabled for this field. See
            // https://github.com/protocolbuffers/protobuf/blob/main/docs/implementing_proto3_presence.md
            String syntax = getParentTypeInfo().getParentFile().getDescriptor().getSyntax();
            switch (syntax) {
                case "proto3":
                    // proto3 initially did not have field presence for primitives. This eventually
                    // turned out to be a mistake, but they couldn't change the default behavior anymore
                    // and added explicit support for opting in to proto2-like field presence. IMO presence
                    // should always be the default, but if we ever officially support proto3, disabling
                    // field presence should:
                    //
                    // * not generate a has method
                    // * not modify bit fields
                    // * serialize and compute size when the field value is not zero
                    //
                    // Note that the code is completely compatible as is. The only difference is that a
                    // zero value may end up being serialized, and that a has method may not return true
                    // if the received value was an omitted zero.
                    return (descriptor.hasProto3Optional() && descriptor.getProto3Optional())
                            || isMessageOrGroup() || isRepeated();
                default:
                case "proto2":
                    // In proto2 everything uses presence by default
                    return true;
            }
        }

        public PluginOptions getPluginOptions() {
            return getParentFile().getParentRequest().getPluginOptions();
        }

        public int getEndGroupTag() {
            return FieldUtil.makeGroupEndTag(tag);
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
            return descriptor.hasDefaultValue();
        }

        public boolean isDeprecated() {
            return descriptor.getOptions().hasDeprecated() && descriptor.getOptions().getDeprecated();
        }

        public TypeName getTypeName() {
            // Lazy because type registry is not constructed at creation time
            return getParentFile().getParentRequest().getTypeRegistry().resolveJavaTypeFromProto(descriptor);
        }

        public boolean isMessageOrGroupWithRequiredFieldsInHierarchy() {
            // Lazy because type registry is not constructed at creation time
            return isMessageOrGroup() && getParentFile().getParentRequest().getTypeRegistry().hasRequiredFieldsInHierarchy(getTypeName());
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

        // Used for the return type in the method, e.g., Optional<String>
        public TypeName getOptionalReturnType() {
            if (isRepeated()) {
                return ParameterizedTypeName.get(ClassName.get(Optional.class), getRepeatedStoreType());
            }
            final TypeName typeName = getTypeName();
            if (!isPrimitive() || typeName == TypeName.BOOLEAN) {
                return ParameterizedTypeName.get(ClassName.get(Optional.class), typeName.box());
            }
            if (typeName == TypeName.INT) return TypeName.get(OptionalInt.class);
            if (typeName == TypeName.LONG) return TypeName.get(OptionalLong.class);
            if (typeName == TypeName.FLOAT) return TypeName.get(OptionalDouble.class);
            if (typeName == TypeName.DOUBLE) return TypeName.get(OptionalDouble.class);
            throw new IllegalArgumentException("Unhandled type: " + typeName);
        }

        // Used for creating the optional, e.g., Optional.of(string)
        public TypeName getOptionalClass() {
            final TypeName type = getOptionalReturnType();
            if (type instanceof ParameterizedTypeName) {
                return ((ParameterizedTypeName) type).rawType;
            }
            return type;
        }

        // Used for JSON serialization (camelCase)
        public String getJsonName() {
            return descriptor.getJsonName();
        }

        // Original field name (under_score). Optional for JSON serialization. Parsers should support both.
        public String getProtoFieldName() {
            return descriptor.getName();
        }

        public String getClearOtherOneOfName() {
            return getContainingOneOf().getClearName() + "Other" + upperName;
        }

        public boolean hasOtherOneOfFields() {
            return descriptor.hasOneofIndex()
                    && getContainingOneOf().getFields().size() > 1;
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

        private OneOfInfo getContainingOneOf() {
            return getParentTypeInfo()
                    .getOneOfs()
                    .get(descriptor.getOneofIndex());
        }

        private final FileInfo parentFile;
        private final MessageInfo parentTypeInfo;
        private final ClassName parentType;
        private final ClassName repeatedStoreType;
        private final FieldDescriptorProto descriptor;
        private final int bitIndex;
        private final String hasBit;
        private final String setBit;
        private final String clearBit;
        private final boolean isPrimitive;
        private final boolean storeUnknownFieldsEnabled;
        private final List<AnnotationSpec> methodAnnotations;
        String fieldName;
        String lowerName;
        String upperName;
        String hazzerName;
        String setterName;
        String getterName;
        String tryGetName;
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
        OneOfInfo(FileInfo parentFile, MessageInfo parentTypeInfo, ClassName parentType, OneofDescriptorProto descriptor, boolean synthetic, int oneOfIndex) {
            this.parentFile = parentFile;
            this.parentTypeInfo = parentTypeInfo;
            this.parentType = parentType;
            this.descriptor = descriptor;
            this.oneOfIndex = oneOfIndex;
            this.synthetic = synthetic;

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
        private final boolean synthetic;
        private final String upperName;
        private final String hazzerName;
        private final String clearName;

    }

}
