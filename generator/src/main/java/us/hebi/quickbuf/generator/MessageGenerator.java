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
                .build());

        // Member state
        BitField.generateMemberFields(type, numBitFields);
        fields.forEach(f -> f.generateMemberFields(type));
        generateUnknownByteMembers(type);

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

    private void generateUnknownByteMembers(TypeSpec.Builder type) {
        if (!info.isStoreUnknownFields()) {
            return;
        }
        type.addField(FieldSpec.builder(RuntimeClasses.BytesType, "unknownBytes")
                .addJavadoc(named("Stores unknown fields to enable message routing without a full definition."))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$T.newEmptyInstance()", RuntimeClasses.BytesType)
                .build());
        type.addMethod(MethodSpec.methodBuilder("getUnknownBytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(RuntimeClasses.BytesType)
                .addStatement("return unknownBytes")
                .build());
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
        BitField.generateClearCode(clear, numBitFields);

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
            equals.addCode("return $L$>", BitField.getEqualsStatement(0));
            for (int i = 1; i < numBitFields; i++) {
                equals.addCode("\n&& $L", BitField.getEqualsStatement(i));
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
        final List<FieldGenerator> sortedFields = getFieldSortedByExpectedIncomingOrder();

        if (enableFallthroughOptimization) {
            mergeFrom.addComment("Enabled Fall-Through Optimization (" + info.getExpectedIncomingOrder() + ")");
        }

        mergeFrom.addStatement(named("int tag = input.readTag()"))
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
                mergeFrom.addCode(named("tag = input.readTag();\n"));
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
        CodeBlock ifSkipField = info.isStoreUnknownFields() ?
                named("if (!input.skipField(tag, $unknownBytes:N))") :
                named("if (!input.skipField(tag))");

        mergeFrom.beginControlFlow("default:")
                .beginControlFlow(ifSkipField)
                .addStatement("return this");
        mergeFrom.endControlFlow()
                .addStatement(named("tag = input.readTag()"))
                .addStatement("break")
                .endControlFlow();

        // Generate missing non-packed cases for packable fields for compatibility reasons
        for (FieldGenerator field : sortedFields) {
            if (field.getInfo().isPackable()) {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getTag());
                boolean readTag = field.generateMergingCode(mergeFrom);
                if (readTag) {
                    mergeFrom.addCode(named("tag = input.readTag();\n"));
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

        boolean needsInitializationChecks = info.hasRequiredFieldsInHierarchy();
        if (needsInitializationChecks) {
            // Fail if any required bits are missing
            insertFailOnMissingRequiredBits(writeTo);
            writeTo.beginControlFlow("try");
        }

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

        if (needsInitializationChecks) {
            writeTo.nextControlFlow("catch ($T nestedFail)", RuntimeClasses.UninitializedMessageException)
                    .addStatement("throw rethrowFromParent(nestedFail)")
                    .endControlFlow();
        }

        type.addMethod(writeTo.build());
    }

    private void generateComputeSerializedSize(TypeSpec.Builder type) {
        MethodSpec.Builder computeSerializedSize = MethodSpec.methodBuilder("computeSerializedSize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(int.class);

        boolean needsInitializationChecks = info.hasRequiredFieldsInHierarchy();
        if (needsInitializationChecks) {
            // Fail if any required bits are missing
            insertFailOnMissingRequiredBits(computeSerializedSize);
            computeSerializedSize.beginControlFlow("try");
        }

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

        if (needsInitializationChecks) {
            computeSerializedSize.nextControlFlow("catch ($T nestedFail)", RuntimeClasses.UninitializedMessageException)
                    .addStatement("throw rethrowFromParent(nestedFail)")
                    .endControlFlow();
        }
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

            // We don't need to copy if neither message has any fields set
            copyFrom.beginControlFlow("if ($L)", BitField.isCopyFromNeeded(fieldIndex));
            BitField.generateCopyFromCode(copyFrom, fieldIndex);
            fields.stream()
                    .filter(field -> BitField.isBitInField(field.info.getBitIndex(), fieldIndex))
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
                .addStatement("return $T.mergeFrom(new $T(), data).checkInitialized()", RuntimeClasses.AbstractMessage, info.getTypeName())
                .build());

        type.addMethod(MethodSpec.methodBuilder("parseFrom")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addException(IOException.class)
                .addParameter(RuntimeClasses.ProtoSource, "input", Modifier.FINAL)
                .returns(info.getTypeName())
                .addStatement("return $T.mergeFrom(new $T(), input).checkInitialized()", RuntimeClasses.AbstractMessage, info.getTypeName())
                .build());
    }

    private void generateIsInitialized(TypeSpec.Builder type) {
        // don't generate it if there is nothing that can be added
        if (!info.hasRequiredFieldsInHierarchy()) {
            return;
        }

        MethodSpec.Builder isInitialized = MethodSpec.methodBuilder("isInitialized")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(boolean.class);

        // Check bits first
        insertOnMissingRequiredBits(isInitialized, m -> m.addStatement("return false"));

        // Check sub-messages (including optional and repeated)
        fields.stream()
                .map(FieldGenerator::getInfo)
                .filter(FieldInfo::isMessageOrGroupWithRequiredFieldsInHierarchy)
                .forEach(field -> {
                    // isInitialized check
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

        // Don't generate lookup if there is no point
        if (fields.stream()
                .map(FieldGenerator::getInfo)
                .noneMatch(field -> field.isRequired() || field.isMessageOrGroup())) {
            return;
        }

        // missing fields lookup
        MethodSpec.Builder getMissingFields = MethodSpec.methodBuilder("getMissingFields")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                .addParameter(String.class, "prefix")
                .addParameter(ParameterizedTypeName.get(List.class, String.class), "results");

        for (FieldGenerator fieldGen : fields) {
            FieldInfo field = fieldGen.getInfo();
            String name = field.getDescriptor().getName();
            CodeBlock checkNestedField = CodeBlock.builder().addStatement(
                    "getMissingFields(prefix, $S, $N, results)",
                    name, field.getFieldName()
            ).build();

            if (field.isRequired()) {
                getMissingFields.beginControlFlow("if (!$N())", field.getHazzerName())
                        .addStatement("results.add(prefix + $S)", name);
                if (field.isMessageOrGroupWithRequiredFieldsInHierarchy()) {
                    getMissingFields.nextControlFlow("else", field.getFieldName())
                            .addCode(checkNestedField);
                }
                getMissingFields.endControlFlow();

            } else if (field.isMessageOrGroupWithRequiredFieldsInHierarchy()) {
                getMissingFields.beginControlFlow("if ($L() && !$N.isInitialized())", field.getHazzerName(), field.getFieldName())
                        .addCode(checkNestedField)
                        .endControlFlow();
            }
        }

        type.addMethod(getMissingFields.build());

    }

    private void insertFailOnMissingRequiredBits(MethodSpec.Builder method) {
        insertOnMissingRequiredBits(method, m -> m.addStatement("throw new $T(this)",
                RuntimeClasses.UninitializedMessageException));
    }

    private void insertOnMissingRequiredBits(MethodSpec.Builder method, Consumer<MethodSpec.Builder> onCondition) {
        // check if all required bits are set
        List<FieldInfo> requiredFields = fields.stream()
                .map(FieldGenerator::getInfo)
                .filter(FieldInfo::isRequired)
                .collect(Collectors.toList());

        if (requiredFields.size() > 0) {
            method.beginControlFlow("if ($L)", BitField.isMissingAnyBit(requiredFields));
            onCondition.accept(method);
            method.endControlFlow();
        }

    }

    private void generateWriteToJson(TypeSpec.Builder type) {
        MethodSpec.Builder writeTo = MethodSpec.methodBuilder("writeTo")
                .addAnnotation(Override.class)
                .addParameter(RuntimeClasses.JsonSink, "output", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class);

        // TODO: should we check for required field initialization? This may mess with toString()
        boolean needsInitializationChecks = info.hasRequiredFieldsInHierarchy();
        if (needsInitializationChecks) {
            // Fail if any required bits are missing
            insertFailOnMissingRequiredBits(writeTo);
            writeTo.beginControlFlow("try");
        }

        writeTo.addStatement("output.beginObject()");

        // add every set field
        fields.forEach(f -> {
            if (f.getInfo().isRequired()) {
                // no need to check has state again
                f.generateJsonSerializationCode(writeTo);
            } else {
                writeTo.beginControlFlow("if ($L)", f.getInfo().getHasBit());
                f.generateJsonSerializationCode(writeTo);
                writeTo.endControlFlow();
            }
        });

        // add unknown fields as base64
        if (info.isStoreUnknownFields()) {
            writeTo.addCode(named("if ($unknownBytes:N.length() > 0)"))
                    .beginControlFlow("")
                    .addStatement(named("output.writeBytes($abstractMessage:T.$unknownBytesKey:N, $unknownBytes:N)"))
                    .endControlFlow();
        }

        writeTo.addStatement("output.endObject()");

        if (needsInitializationChecks) {
            writeTo.nextControlFlow("catch ($T nestedFail)", RuntimeClasses.UninitializedMessageException)
                    .addStatement("throw rethrowFromParent(nestedFail)")
                    .endControlFlow();
        }
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

        // Fallthrough optimization:
        //
        // Reads tag after case parser and checks if it can fall-through. In the ideal case if all fields are set
        // and the expected order matches the incoming data, the switch would only need to be executed once
        // for the first field.
        // TODO: On Jackson/Gson sources this has negligible benefits. Deactivate for now to keep the code cleaner.
        final boolean enableFallthroughOptimization = false; // info.getExpectedIncomingOrder() != ExpectedIncomingOrder.None;
        final List<FieldGenerator> sortedFields = fields; // getFieldSortedByExpectedIncomingOrder();

        if (enableFallthroughOptimization) {
            mergeFrom.addStatement("int hash = input.nextFieldHashOrZero()")
                    .beginControlFlow("while (true)")
                    .beginControlFlow("switch (hash)");
        } else {
            mergeFrom.beginControlFlow("while (input.hasNext())")
                    .beginControlFlow("switch (input.nextFieldHash())");
        }

        // add case statements for every field
        for (int i = 0; i < sortedFields.size(); i++) {
            FieldGenerator field = sortedFields.get(i);

            // Synonym hash check
            int hash1 = FieldUtil.hash32(field.getInfo().getJsonName());
            int hash2 = FieldUtil.hash32(field.getInfo().getProtoFieldName());
            if (hash1 != hash2) {
                mergeFrom.addCode("case $L:\n", hash1);
            }

            // Known hash -> try to parse
            mergeFrom.beginControlFlow("case $L:", hash2)
                    .beginControlFlow("if (input.isAtField($N.$N))",
                            info.getFieldNamesClass().simpleName(),
                            field.getInfo().getFieldName());
            field.generateJsonDeserializationCode(mergeFrom);

            // Unknown field -> skip
            mergeFrom.nextControlFlow("else")
                    .addStatement("input.skipUnknownField()")
                    .endControlFlow();

            // See if we can fallthrough to the next case
            if (enableFallthroughOptimization) {
                // Find hashes of next entry
                int nextHash1 = 0;
                int nextHash2 = 0;
                if (i + 1 < sortedFields.size()) {
                    FieldGenerator nextField = sortedFields.get(i + 1);
                    nextHash1 = FieldUtil.hash32(nextField.getInfo().getJsonName());
                    nextHash2 = FieldUtil.hash32(nextField.getInfo().getProtoFieldName());
                }

                // Check if we can fall through
                mergeFrom.addStatement("hash = input.nextFieldHashOrZero()");
                if (nextHash1 == nextHash2) {
                    mergeFrom.beginControlFlow("if (hash != $L)", nextHash1)
                            .addStatement("break")
                            .endControlFlow();
                } else {
                    mergeFrom.beginControlFlow("if (hash != $L && hash != $L)", nextHash1, nextHash2)
                            .addStatement("break")
                            .endControlFlow();
                }

            } else {
                // Always go to main switch
                mergeFrom.addStatement("break");
            }
            mergeFrom.endControlFlow();

        }

        // add zero case -> no more entries
        if (enableFallthroughOptimization) {
            mergeFrom.beginControlFlow("case 0:")
                    .addStatement("input.endObject()")
                    .addStatement("return this")
                    .endControlFlow();
        }

        // add unknown bytes
        if (info.isStoreUnknownFields()) {
            mergeFrom
                    .addCode("case $L:\n", RuntimeClasses.unknownBytesFieldHash1)
                    .beginControlFlow("case $L:", RuntimeClasses.unknownBytesFieldHash2)
                    .beginControlFlow("if (input.isAtField($T.$N))",
                            RuntimeClasses.AbstractMessage, RuntimeClasses.unknownBytesFieldName)
                    // TODO: this should actually be parsing the bytes in case the message definition is different
                    .addStatement("input.nextBase64($N)", RuntimeClasses.unknownBytesField)
                    .nextControlFlow("else")
                    .addStatement("input.skipUnknownField()")
                    .endControlFlow();
            if (enableFallthroughOptimization) {
                mergeFrom.addStatement("hash = input.nextFieldHashOrZero()");
            }
            mergeFrom.addStatement("break");
            mergeFrom.endControlFlow();
        }

        // add default case
        mergeFrom.beginControlFlow("default:")
                .addStatement("input.skipUnknownField()");
        if (enableFallthroughOptimization) {
            mergeFrom.addStatement("hash = input.nextFieldHashOrZero()");
        }
        mergeFrom.addStatement("break")
                .endControlFlow() // case
                .endControlFlow() // switch
                .endControlFlow(); // while

        if (!enableFallthroughOptimization) {
            mergeFrom.addStatement("input.endObject()")
                    .addStatement("return this");
        }

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
            FieldSpec.Builder field = FieldSpec.builder(RuntimeClasses.FieldName, f.getInfo().getFieldName(), Modifier.STATIC, Modifier.FINAL);
            if (f.getInfo().getJsonName().equals(f.getInfo().getProtoFieldName())) {
                field.initializer("$T.forField($S)", RuntimeClasses.FieldName, f.getInfo().getJsonName());
            } else {
                field.initializer("$T.forField($S, $S)", RuntimeClasses.FieldName,
                        f.getInfo().getJsonName(), f.getInfo().getProtoFieldName());
            }
            fieldNamesClass.addField(field.build());
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

    private List<FieldGenerator> getFieldSortedByExpectedIncomingOrder() {
        final List<FieldGenerator> sortedFields = new ArrayList<>(fields);
        switch (info.getExpectedIncomingOrder()) {
            case AscendingNumber:
                sortedFields.sort(FieldUtil.AscendingNumberSorter);
                break;
            case Quickbuf: // keep existing order
            case None: // no optimization
                break;
        }
        return sortedFields;
    }

    final MessageInfo info;
    final List<FieldGenerator> fields = new ArrayList<>();
    final int numBitFields;
    final HashMap<String, Object> m = new HashMap<>();

    private CodeBlock named(String format, Object... args /* makes IDE hints disappear */) {
        return CodeBlock.builder().addNamed(format, m).build();
    }

}
