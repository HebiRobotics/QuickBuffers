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

import com.squareup.javapoet.*;
import us.hebi.quickbuf.generator.RequestInfo.ExpectedIncomingOrder;
import us.hebi.quickbuf.generator.RequestInfo.FieldInfo;
import us.hebi.quickbuf.generator.RequestInfo.MessageInfo;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class MessageGenerator {

    TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.classBuilder(info.getTypeName())
                .superclass(ParameterizedTypeName.get(RuntimeClasses.AbstractMessage, info.getTypeName()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        if (info.isNested()) {
            type.addModifiers(Modifier.STATIC);
        }

        if (!info.isNested()) {
            // Note: constants from enums and fields may have the same names
            // as constants in the nested classes. This causes Java warnings,
            // but is not fatal, so we suppress those warnings in the top-most
            // class declaration /javanano
            type.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "hiding")
                    .build());
        }

        // Nested Enums
        info.getNestedEnums().stream()
                .map(EnumGenerator::new)
                .map(EnumGenerator::generate)
                .forEach(type::addType);

        // Nested Types
        info.getNestedTypes().stream()
                .map(MessageGenerator::new)
                .map(MessageGenerator::generate)
                .forEach(type::addType);

        // newInstance() method
        type.addMethod(MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(info.getTypeName())
                .addStatement("return new $T()", info.getTypeName())
                .build());

        // Constructor
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("super($L)", info.isStoreUnknownFields())
                .build());

        // Member state (the first bitfield is in the parent class)
        for (int i = 1; i < numBitFields; i++) {
            type.addField(FieldSpec.builder(int.class, BitField.fieldName(i), Modifier.PRIVATE).build());
        }
        fields.forEach(f -> f.generateMemberFields(type));

        // OneOf Accessors
        info.getOneOfs().stream()
                .map(OneOfGenerator::new)
                .forEach(oneOf -> oneOf.generateMemberMethods(type));

        // Fields accessors
        fields.forEach(f -> f.generateMemberMethods(type));
        generateCopyFrom(type);
        generateMergeFromMessage(type);
        generateClear(type);
        generateEquals(type);
        generateWriteTo(type);
        generateComputeSerializedSize(type);
        generateMergeFrom(type);
        generateIsInitialized(type);
        generateWriteToJson(type);
        if (info.getParentFile().getParentRequest().generateMergeFromJson()) {
            generateMergeFromJson(type);
        }
        generateClone(type);

        // Utility methods
        generateIsEmpty(type);

        // Static utilities
        generateParseFrom(type);
        generateMessageFactory(type);
        generateJsonFieldNames(type);
        type.addField(FieldSpec.builder(TypeName.LONG, "serialVersionUID")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("0L")
                .build());

        return type.build();
    }

    private void generateClear(TypeSpec.Builder type) {
        type.addMethod(generateClearCode("clear", true));
        type.addMethod(generateClearCode("clearQuick", false));
    }

    private void generateIsEmpty(TypeSpec.Builder type) {
        MethodSpec.Builder isEmpty = MethodSpec.methodBuilder("isEmpty")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return $N", BitField.hasNoBits(numBitFields));
        type.addMethod(isEmpty.build());
    }

    private MethodSpec generateClearCode(String name, boolean isFullClear) {
        MethodSpec.Builder clear = MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());

        // no fields set -> no need to clear (e.g. unused nested messages)
        // NOTE: always make sure that the constructor creates conditions that clears everything
        clear.beginControlFlow("if (isEmpty())")
                .addStatement("return this")
                .endControlFlow();

        // clear has state
        clear.addStatement("cachedSize = -1");
        for (int i = 0; i < numBitFields; i++) {
            clear.addStatement("$L = 0", BitField.fieldName(i));
        }

        if (isFullClear) {
            fields.forEach(field -> field.generateClearCode(clear));
        } else {
            fields.forEach(field -> field.generateClearQuickCode(clear));
        }

        if (info.isStoreUnknownFields()) {
            clear.addStatement(named("$unknownBytes:N.clear()"));
        }

        clear.addStatement("return this");
        return clear.build();
    }

    private void generateEquals(TypeSpec.Builder type) {
        MethodSpec.Builder equals = MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "o");

        // Reference equality check
        equals.beginControlFlow("if (o == this)")
                .addStatement("return true")
                .endControlFlow();

        // Type check
        equals.beginControlFlow("if (!(o instanceof $T))", info.getTypeName())
                .addStatement("return false")
                .endControlFlow();
        equals.addStatement("$1T other = ($1T) o", info.getTypeName());

        // Check whether all of the same fields are set
        if (info.getFieldCount() > 0) {
            equals.addCode("return $1L == other.$1L$>", BitField.fieldName(0));
            for (int i = 1; i < numBitFields; i++) {
                equals.addCode("\n&& $1L == other.$1L", BitField.fieldName(i));
            }

            for (FieldGenerator field : fields) {
                equals.addCode("\n&& (!$1N() || ", field.getInfo().getHazzerName());
                field.generateEqualsStatement(equals);
                equals.addCode(")");
            }

            equals.addCode(";$<\n");
        } else {
            equals.addCode("return true;\n");
        }

        type.addMethod(equals.build());
    }

    private void generateMergeFrom(TypeSpec.Builder type) {
        MethodSpec.Builder mergeFrom = MethodSpec.methodBuilder("mergeFrom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addParameter(RuntimeClasses.ProtoSource, "input", Modifier.FINAL)
                .addException(IOException.class);

        // Fallthrough optimization:
        //
        // Reads tag after case parser and checks if it can fall-through. In the ideal case if all fields are set
        // and the expected order matches the incoming data, the switch would only need to be executed once
        // for the first field.
        //
        // Packable fields make this a bit more complex since they need to generate two cases to preserve
        // backwards compatibility. However, any production proto file should already be using the packed
        // option whenever possible, so we don't need to optimize the non-packed case.
        final boolean enableFallthroughOptimization = info.getExpectedIncomingOrder() != ExpectedIncomingOrder.None;
        final List<FieldGenerator> sortedFields = new ArrayList<>(fields);
        switch (info.getExpectedIncomingOrder()) {
            case AscendingNumber:
                sortedFields.sort(FieldUtil.AscendingNumberSorter);
                break;
            case Quickbuf: // keep existing order
            case None: // no optimization
                break;
        }
        if (enableFallthroughOptimization) {
            mergeFrom.addComment("Enabled Fall-Through Optimization (" + info.getExpectedIncomingOrder() + ")");
        }

        m.put("readTag", info.isStoreUnknownFields() ? "readTagMarked" : "readTag");

        mergeFrom.addStatement(named("int tag = input.$readTag:N()"))
                .beginControlFlow("while (true)")
                .beginControlFlow("switch (tag)");

        // Add fields by the expected order and type
        for (int i = 0; i < sortedFields.size(); i++) {
            FieldGenerator field = sortedFields.get(i);

            // Assume all packable fields are written packed. Add non-packed cases to the end.
            boolean readTag = true;
            if (field.getInfo().isPackable()) {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getPackedTag());
                readTag = field.generateMergingCodeFromPacked(mergeFrom);
            } else {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getTag());
                readTag = field.generateMergingCode(mergeFrom);
            }

            if (readTag) {
                mergeFrom.addCode(named("tag = input.$readTag:N();\n"));
            }

            if (enableFallthroughOptimization) {
                // try falling to 0 (exit) at last field
                final int nextCase = (i == sortedFields.size() - 1) ? 0 : getPackedTagOrTag(sortedFields.get(i + 1));
                mergeFrom.beginControlFlow("if (tag != $L)", nextCase);
                mergeFrom.addStatement("break");
                mergeFrom.endControlFlow();
            } else {
                mergeFrom.addStatement("break");
            }
            mergeFrom.endControlFlow();

        }

        // zero means invalid tag / end of data
        mergeFrom.beginControlFlow("case 0:")
                .addStatement("return this")
                .endControlFlow();

        // default case -> skip field
        mergeFrom.beginControlFlow("default:")
                .beginControlFlow("if (!input.skipField(tag))")
                .addStatement("return this");
        if (info.isStoreUnknownFields()) {
            mergeFrom.nextControlFlow("else")
                    .addStatement(named("input.copyBytesSinceMark($unknownBytes:N)"));
        }
        mergeFrom.endControlFlow()
                .addStatement(named("tag = input.$readTag:N()"))
                .addStatement("break")
                .endControlFlow();

        // Generate missing non-packed cases for packable fields for compatibility reasons
        for (FieldGenerator field : sortedFields) {
            if (field.getInfo().isPackable()) {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getTag());
                boolean readTag = field.generateMergingCode(mergeFrom);
                if (readTag) {
                    mergeFrom.addCode(named("tag = input.$readTag:N();\n"));
                }
                mergeFrom.addStatement("break").endControlFlow();
            }
        }

        mergeFrom.endControlFlow();
        mergeFrom.endControlFlow();
        type.addMethod(mergeFrom.build());
    }

    private int getPackedTagOrTag(FieldGenerator field) {
        if (field.getInfo().isPackable())
            return field.getInfo().getPackedTag();
        return field.getInfo().getTag();
    }

    private void generateWriteTo(TypeSpec.Builder type) {
        MethodSpec.Builder writeTo = MethodSpec.methodBuilder("writeTo")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(RuntimeClasses.ProtoSink, "output", Modifier.FINAL)
                .addException(IOException.class);

        // Fail if any required bits are missing
        insertFailOnMissingRequiredBits(writeTo);

        fields.forEach(f -> {
            if (f.getInfo().isRequired()) {
                // no need to check has state again
                f.generateSerializationCode(writeTo);
            } else {
                writeTo.beginControlFlow("if ($L)", f.getInfo().getHasBit());
                f.generateSerializationCode(writeTo);
                writeTo.endControlFlow();
            }
        });
        if (info.isStoreUnknownFields()) {
            writeTo.addCode(named("if ($unknownBytes:N.length() > 0)"))
                    .beginControlFlow("")
                    .addStatement(named("output.writeRawBytes($unknownBytes:N.array(), 0, $unknownBytes:N.length())"))
                    .endControlFlow();
        }
        type.addMethod(writeTo.build());
    }

    private void generateComputeSerializedSize(TypeSpec.Builder type) {
        MethodSpec.Builder computeSerializedSize = MethodSpec.methodBuilder("computeSerializedSize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(int.class);

        // Fail if any required bits are missing
        insertFailOnMissingRequiredBits(computeSerializedSize);

        // Check all required fields at once
        computeSerializedSize.addStatement("int size = 0");
        fields.forEach(f -> {
            if (f.getInfo().isRequired()) {
                // no need to check has state again
                f.generateComputeSerializedSizeCode(computeSerializedSize);
            } else {
                computeSerializedSize.beginControlFlow("if ($L)", f.getInfo().getHasBit());
                f.generateComputeSerializedSizeCode(computeSerializedSize);
                computeSerializedSize.endControlFlow();
            }
        });
        if (info.isStoreUnknownFields()) {
            computeSerializedSize.addStatement(named("size += $unknownBytes:N.length()"));
        }
        computeSerializedSize.addStatement("return size");
        type.addMethod(computeSerializedSize.build());
    }

    private void generateCopyFrom(TypeSpec.Builder type) {
        MethodSpec.Builder copyFrom = MethodSpec.methodBuilder("copyFrom")
                .addAnnotation(Override.class)
                .addParameter(info.getTypeName(), "other", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());
        copyFrom.addStatement("cachedSize = other.cachedSize");
        for (int i = 0; i < numBitFields; i++) {
            final int fieldIndex = i;
            String bitField = BitField.fieldName(fieldIndex);

            // We don't need to copy if neither message has any fields set
            copyFrom.beginControlFlow("if (($1L | other.$1L) != 0)", bitField);
            copyFrom.addStatement("$1L = other.$1L", bitField);
            fields.stream()
                    .filter(field -> BitField.getFieldIndex(field.info.getBitIndex()) == fieldIndex)
                    .forEach(field -> field.generateCopyFromCode(copyFrom));
            copyFrom.endControlFlow();
        }

        if (info.isStoreUnknownFields()) {
            copyFrom.addStatement(named("$unknownBytes:N.copyFrom(other.$unknownBytes:N)"));
        }
        copyFrom.addStatement("return this");
        type.addMethod(copyFrom.build());
    }

    private void generateMergeFromMessage(TypeSpec.Builder type) {
        MethodSpec.Builder mergeFrom = MethodSpec.methodBuilder("mergeFrom")
                .addAnnotation(Override.class)
                .addParameter(info.getTypeName(), "other", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());

        mergeFrom.beginControlFlow("if (other.isEmpty())")
                .addStatement("return this")
                .endControlFlow();
        mergeFrom.addStatement("cachedSize = -1");

        fields.forEach(field -> {
            mergeFrom.beginControlFlow("if (other.$L())", field.getInfo().getHazzerName());
            field.generateMergeFromMessageCode(mergeFrom);
            mergeFrom.endControlFlow();
        });

        if (info.isStoreUnknownFields()) {
            mergeFrom.beginControlFlow("$L", named("if (other.$unknownBytes:N.length() > 0)"))
                    .addStatement(named("$unknownBytes:N.addAll(other.$unknownBytes:N)"))
                    .endControlFlow();
        }
        mergeFrom.addStatement("return this");
        type.addMethod(mergeFrom.build());
    }

    private void generateClone(TypeSpec.Builder type) {
        type.addSuperinterface(Cloneable.class);
        type.addMethod(MethodSpec.methodBuilder("clone")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addStatement("return new $T().copyFrom(this)", info.getTypeName())
                .build());
    }

    private void generateParseFrom(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder("parseFrom")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addException(RuntimeClasses.InvalidProtocolBufferException)
                .addParameter(byte[].class, "data", Modifier.FINAL)
                .returns(info.getTypeName())
                .addStatement("return $T.mergeFrom(new $T(), data)", RuntimeClasses.AbstractMessage, info.getTypeName())
                .build());
    }

    private void generateIsInitialized(TypeSpec.Builder type) {
        MethodSpec.Builder isInitialized = MethodSpec.methodBuilder("isInitialized")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(boolean.class);

        // Check bits first
        insertOnMissingRequiredBits(isInitialized, m -> m.addStatement("return false"));

        // Check sub-messages (including optional and repeated)
        fields.stream()
                .map(FieldGenerator::getInfo)
                .filter(FieldInfo::isMessageOrGroup)
                .forEach(field -> {
                    if (field.isRequired()) {
                        // has bit was already checked
                        isInitialized.beginControlFlow("if (!$N.isInitialized())", field.getFieldName());
                    } else {
                        // We need to check has bit ourselves
                        isInitialized.beginControlFlow("if ($L() && !$N.isInitialized())", field.getHazzerName(), field.getFieldName());
                    }
                    isInitialized
                            .addStatement("return false")
                            .endControlFlow();
                });

        isInitialized.addStatement("return true");
        type.addMethod(isInitialized.build());
    }

    private void insertFailOnMissingRequiredBits(MethodSpec.Builder method) {
        String error = "Message is missing at least one required field";
        insertOnMissingRequiredBits(method, m -> m.addStatement("throw new $T($S)", IllegalStateException.class, error));
    }

    private void insertOnMissingRequiredBits(MethodSpec.Builder method, Consumer<MethodSpec.Builder> onCondition) {
        // check if all required bits are set
        List<FieldInfo> requiredFields = fields.stream()
                .map(FieldGenerator::getInfo)
                .filter(FieldInfo::isRequired)
                .collect(Collectors.toList());

        if (requiredFields.size() > 0) {
            int[] bitset = BitField.generateBitset(requiredFields);
            method.beginControlFlow("if ($L)", BitField.isMissingAnyBit(bitset));
            onCondition.accept(method);
            method.endControlFlow();
        }

    }

    private void generateWriteToJson(TypeSpec.Builder type) {
        MethodSpec.Builder writeTo = MethodSpec.methodBuilder("writeTo")
                .addAnnotation(Override.class)
                .addParameter(RuntimeClasses.JsonSink, "output", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addStatement("output.beginObject()");

        // add every set field
        for (FieldGenerator field : fields) {
            writeTo.beginControlFlow("if ($L)", BitField.hasBit(field.getInfo().getBitIndex()));
            field.generateJsonSerializationCode(writeTo);
            writeTo.endControlFlow();
        }

        // add unknown fields as base64
        if (info.isStoreUnknownFields()) {
            writeTo.addCode(named("if ($unknownBytes:N.length() > 0)"))
                    .beginControlFlow("")
                    .addStatement(named("output.writeBytes($abstractMessage:T.$unknownBytesKey:N, $unknownBytes:N)"))
                    .endControlFlow();
        }

        writeTo.addStatement("output.endObject()");
        type.addMethod(writeTo.build());
    }

    private void generateMergeFromJson(TypeSpec.Builder type) {
        MethodSpec.Builder mergeFrom = MethodSpec.methodBuilder("mergeFrom")
                .addAnnotation(Override.class)
                .returns(info.getTypeName())
                .addParameter(RuntimeClasses.JsonSource, "input", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class);

        mergeFrom.beginControlFlow("if (!input.beginObject())")
                .addStatement("return this")
                .endControlFlow();

        mergeFrom.beginControlFlow("while (input.hasNext())")
                .beginControlFlow("switch (input.nextFieldHash())");

        // add case statements for every field
        for (FieldGenerator field : fields) {
            Consumer<String> generateCase = (name) -> {
                final int hash = FieldUtil.hash32(name);
                mergeFrom.beginControlFlow("case $L:", hash)
                        .beginControlFlow("if (input.isAtField($S))", name);
                field.generateJsonDeserializationCode(mergeFrom);
                mergeFrom.nextControlFlow("else")
                        .addStatement("input.skipValue()")
                        .endControlFlow()
                        .addStatement("break")
                        .endControlFlow();
            };

            String name1 = field.getInfo().getPrimaryJsonName();
            generateCase.accept(name1);

            String name2 = field.getInfo().getSecondaryJsonName();
            if (!name1.equals(name2)) {
                generateCase.accept(name2);
            }
        }

        // add default case
        mergeFrom.beginControlFlow("default:")
                .addStatement("input.skipValue()")
                .addStatement("break")
                .endControlFlow();

        mergeFrom.endControlFlow()
                .endControlFlow()
                .addStatement("input.endObject()")
                .addStatement("return this");

        type.addMethod(mergeFrom.build());
    }

    private void generateMessageFactory(TypeSpec.Builder type) {
        ParameterizedTypeName factoryReturnType = ParameterizedTypeName.get(RuntimeClasses.MessageFactory, info.getTypeName());
        ClassName factoryTypeName = info.getTypeName().nestedClass(info.getTypeName().simpleName() + "Factory");

        MethodSpec factoryMethod = MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addStatement("return $T.newInstance()", info.getTypeName())
                .build();

        TypeSpec factoryEnum = TypeSpec.enumBuilder(factoryTypeName.simpleName())
                .addModifiers(Modifier.PRIVATE)
                .addSuperinterface(factoryReturnType)
                .addEnumConstant("INSTANCE")
                .addMethod(factoryMethod)
                .build();

        type.addType(factoryEnum);

        type.addMethod(MethodSpec.methodBuilder("getFactory")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(factoryReturnType)
                .addStatement("return $T.INSTANCE", factoryTypeName)
                .build());

    }

    private void generateJsonFieldNames(TypeSpec.Builder type) {
        TypeSpec.Builder fieldNamesClass = TypeSpec.classBuilder(info.getFieldNamesClass().simpleName())
                .addJavadoc("Contains name constants used for serializing JSON\n")
                .addModifiers(Modifier.STATIC);

        fields.forEach(f -> {
            fieldNamesClass.addField(FieldSpec.builder(RuntimeClasses.FieldName, f.getInfo().getFieldName(), Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.forField($S)", RuntimeClasses.FieldName, f.getInfo().getPrimaryJsonName())
                    .build());
        });

        type.addType(fieldNamesClass.build());
    }

    MessageGenerator(MessageInfo info) {
        this.info = info;
        info.getFields().forEach(f -> fields.add(new FieldGenerator(f)));
        numBitFields = info.getNumBitFields();

        m.put("abstractMessage", RuntimeClasses.AbstractMessage);
        m.put("unknownBytes", RuntimeClasses.unknownBytesField);
        m.put("unknownBytesKey", RuntimeClasses.unknownBytesFieldName);
    }

    final MessageInfo info;
    final List<FieldGenerator> fields = new ArrayList<>();
    final int numBitFields;
    final HashMap<String, Object> m = new HashMap<>();

    private CodeBlock named(String format, Object... args /* makes IDE hints disappear */) {
        return CodeBlock.builder().addNamed(format, m).build();
    }

}
