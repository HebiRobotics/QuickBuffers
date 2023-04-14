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

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * This class generates all serialization logic and field related accessors.
 * It is a bit of a mess due to lots of switch statements, but I found that
 * splitting the types up similarly to how the protobuf-generator code is
 * organized makes it really difficult to find and manage duplicate code,
 * and to keep track of where things are being called.
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerator {

    protected RequestInfo.FieldInfo getInfo() {
        return info;
    }

    protected void generateMemberFields(TypeSpec.Builder type) {
        FieldSpec.Builder field = FieldSpec.builder(storeType, info.getFieldName())
                .addJavadoc(named("$commentLine:L"))
                .addModifiers(Modifier.PRIVATE);

        if (info.isLazyAllocationEnabled()) {
            field.initializer("null");
        } else if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes() || info.isString()) {
            field.addModifiers(Modifier.FINAL).initializer(initializer());
        } else if (info.isPrimitive() || info.isEnum()) {
            if (info.hasDefaultValue()) {
                field.initializer(initializer());
            }
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
        type.addField(field.build());

        if (info.isBytes() && info.hasDefaultValue()) {
            // byte[] default values are stored as utf8 strings, so we need to convert it first
            type.addField(FieldSpec.builder(ArrayTypeName.get(byte[].class), info.getDefaultFieldName())
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(named("$abstractMessage:T.bytesDefaultValue(\"$default:L\")"))
                    .build());
        }
    }

    private CodeBlock initializer() {
        CodeBlock.Builder initializer = CodeBlock.builder();
        if (info.isRepeated() && info.isMessageOrGroup()) {
            initializer.add("$T.newEmptyInstance($T.getFactory())", RuntimeClasses.RepeatedMessage, info.getTypeName());
        } else if (info.isRepeated() && info.isEnum()) {
            initializer.add("$T.newEmptyInstance($T.converter())", RuntimeClasses.RepeatedEnum, info.getTypeName());
        } else if (info.isRepeated()) {
            initializer.add("$T.newEmptyInstance()", storeType);
        } else if (info.isBytes()) {
            if (!info.hasDefaultValue()) {
                initializer.add(named("$storeType:T.newEmptyInstance()"));
            } else {
                initializer.add(named("$storeType:T.newInstance($defaultField:N)"));
            }
        } else if (info.isMessageOrGroup()) {
            initializer.add(named("$storeType:T.newInstance()"));
        } else if (info.isString()) {
            if (!info.hasDefaultValue()) {
                initializer.add(named("$storeType:T.newEmptyInstance()"));
            } else {
                initializer.add(named("$storeType:T.newInstance($default:S)"));
            }
        } else if (info.isPrimitive() || info.isEnum()) {
            if (info.hasDefaultValue()) {
                initializer.add(named("$default:L"));
            }
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
        return initializer.build();
    }

    protected void generateClearCode(MethodSpec.Builder method) {
        if (info.isSingularPrimitiveOrEnum()) {
            method.addStatement(named("$field:N = $default:L"));
            return;
        }

        if (info.isLazyAllocationEnabled()) {
            method.beginControlFlow(named("if ($field:N != null)"));
        }

        if (info.isRepeated() || info.isMessageOrGroup()) {
            method.addStatement(named("$field:N.clear()"));

        } else if (info.isString()) {
            if (info.hasDefaultValue()) {
                method.addStatement(named("$field:N.copyFrom($default:S)"));
            } else {
                method.addStatement(named("$field:N.clear()"));
            }
        } else if (info.isBytes()) {
            if (info.hasDefaultValue()) {
                method.addStatement(named("$field:N.copyFrom($defaultField:N)"));
            } else {
                method.addStatement(named("$field:N.clear()"));
            }
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }

        if (info.isLazyAllocationEnabled()) {
            method.endControlFlow();
        }
    }

    protected void generateClearQuickCode(MethodSpec.Builder method) {
        if (info.isSingularPrimitiveOrEnum()) {
            return; // no action needed
        }

        if (info.isLazyAllocationEnabled()) {
            method.beginControlFlow(named("if ($field:N != null)"));
        }

        if (info.isMessageOrGroup()) { // includes repeated messages
            method.addStatement(named("$field:N.clearQuick()"));
        } else if (info.isRepeated() || info.isBytes() || info.isString()) {
            method.addStatement(named("$field:N.clear()"));
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }

        if (info.isLazyAllocationEnabled()) {
            method.endControlFlow();
        }
    }

    protected void generateCopyFromCode(MethodSpec.Builder method) {
        if (info.isSingularPrimitiveOrEnum()) {
            method.addStatement(named("$field:N = other.$field:N"));

        } else if (info.isRepeated() || info.isBytes() || info.isMessageOrGroup() || info.isString()) {
            if (info.isLazyAllocationEnabled()) {
                method.addCode(named("" +
                        "if (other.$hasMethod:N()) {$>\n" +
                        "$lazyInitMethod:L();\n" +
                        "$field:N.copyFrom(other.$field:N);\n" +
                        "$<} else {$>\n" +
                        "$clearMethod:L();\n" +
                        "$<}\n"));
            } else {
                method.addStatement(named("$field:N.copyFrom(other.$field:N)"));
            }
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    protected void generateMergeFromMessageCode(MethodSpec.Builder method) {
        if (info.isRepeated()) {
            method.addStatement(named("$getMutableMethod:N().addAll(other.$field:N)"));
        } else if (info.isMessageOrGroup()) {
            method.addStatement(named("$getMutableMethod:N().mergeFrom(other.$field:N)"));
        } else if (info.isBytes()) {
            method.addStatement(named("$getMutableMethod:N().copyFrom(other.$field:N)"));
        } else if (info.isString()) {
            method.addStatement(named("$getMutableMethod:NBytes().copyFrom(other.$field:N)"));
        } else if (info.isEnum()) {
            method.addStatement(named("$setMethod:NValue(other.$field:N)"));
        } else if (info.isPrimitive()) {
            method.addStatement(named("$setMethod:N(other.$field:N)"));
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    protected void generateEqualsStatement(MethodSpec.Builder method) {
        if (info.isRepeated() || info.isBytes() || info.isMessageOrGroup() || info.isString()) {
            method.addNamedCode("$field:N.equals(other.$field:N)", m);

        } else if (typeName == TypeName.DOUBLE || typeName == TypeName.FLOAT) {
            method.addNamedCode("$protoUtil:T.isEqual($field:N, other.$field:N)", m);

        } else if (info.isPrimitive() || info.isEnum()) {
            method.addNamedCode("$field:N == other.$field:N", m);

        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    /**
     * @return true if the tag needs to be read
     */
    protected boolean generateMergingCode(MethodSpec.Builder method) {
        method.addCode(clearOtherOneOfs).addCode(ensureFieldNotNull);
        if (info.isRepeated()) {
            method
                    .addNamedCode("tag = input.readRepeated$capitalizedType:L($field:N, tag);\n", m)
                    .addStatement(named("$setHas:L"));
            return false; // tag is already read, so don't read again

        } else if (info.isString() || info.isMessageOrGroup() || info.isBytes()) {
            method
                    .addStatement(named("input.read$capitalizedType:L($field:N$secondArgs:L)"))
                    .addStatement(named("$setHas:L"));

        } else if (info.isPrimitive()) {
            method
                    .addStatement(named("$field:N = input.read$capitalizedType:L()"))
                    .addStatement(named("$setHas:L"));

        } else if (info.isEnum()) {
            method
                    .addStatement("final int value = input.readInt32()")
                    .beginControlFlow("if ($T.forNumber(value) != null)", typeName)
                    .addStatement(named("$field:N = value"))
                    .addStatement(named("$setHas:L"));

            // NOTE:
            //  Google's Protobuf-Java selectively moves repeated enum values that it does not know.
            //  This is problematic when going through a routing node as it may change the order of the
            //  data by sorting it as known values followed by unknown values. Even though this is
            //  the specified behavior, I don't think that this is desired and would rather have users
            //  deal with potential null values.
            if (info.isStoreUnknownFieldsEnabled()) {
                method.nextControlFlow("else")
                        .addStatement("input.skipEnum(tag, value, $N)", RuntimeClasses.unknownBytesField);
            }

            method.endControlFlow();

        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
        return true;
    }

    /**
     * @return true if the tag needs to be read
     */
    protected boolean generateMergingCodeFromPacked(MethodSpec.Builder method) {
        if (!info.isPackable()) {
            throw new IllegalStateException("not a packable type: " + info.getDescriptor());
        }
        method.addCode(clearOtherOneOfs).addCode(ensureFieldNotNull);
        if (info.isFixedWidth()) {
            method.addStatement(named("input.readPacked$capitalizedType:L($field:N)"));
        } else {
            method.addStatement(named("input.readPacked$capitalizedType:L($field:N, tag)"));
        }
        method.addStatement(named("$setHas:L"));
        return true;
    }

    protected void generateSerializationCode(MethodSpec.Builder method) {
        m.put("writeTagToOutput", generateWriteVarint32(getInfo().getTag()));
        if (info.isPacked()) {
            m.put("writePackedTagToOutput", generateWriteVarint32(getInfo().getPackedTag()));
        }
        m.put("writeEndGroupTagToOutput", !info.isGroup() ? "" :
                generateWriteVarint32(getInfo().getEndGroupTag()));

        if (info.isPacked()) {
            method.addNamedCode("" +
                    "$writePackedTagToOutput:L" +
                    "output.writePacked$capitalizedType:LNoTag($field:N);\n", m);

        } else if (info.isRepeated()) {
            method.addNamedCode("" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "$writeTagToOutput:L" +
                    "output.write$capitalizedType:LNoTag($field:N.$getRepeatedIndex_i:L);\n" +
                    "$writeEndGroupTagToOutput:L" +
                    "$<}\n", m);

        } else {
            // unroll varint tag loop
            method.addNamedCode("" + // non-repeated
                    "$writeTagToOutput:L" +
                    "output.write$capitalizedType:LNoTag($field:N);\n" +
                    "$writeEndGroupTagToOutput:L", m
            );
        }
    }

    private static String generateWriteVarint32(int value) {
        // Split tag into individual bytes
        int[] bytes = new int[5];
        int numBytes = 0;
        while (true) {
            if ((value & ~0x7F) == 0) {
                bytes[numBytes++] = value;
                break;
            } else {
                bytes[numBytes++] = (value & 0x7F) | 0x80;
                value >>>= 7;
            }
        }

        // Write tag bytes as efficiently as possible
        String output = "";
        switch (numBytes) {

            case 4:
            case 5:
                final int fourBytes = (bytes[3] << 24 | bytes[2] << 16 | bytes[1] << 8 | bytes[0]);
                output = "output.writeRawLittleEndian32(" + fourBytes + ");\n";
                if (numBytes == 5) output += "output.writeRawByte((byte) " + bytes[4] + ");\n";
                break;

            case 2:
            case 3:
                final int twoBytes = (bytes[1] << 8 | bytes[0]);
                output = "output.writeRawLittleEndian16((short) " + twoBytes + ");\n";
                if (numBytes == 3) output += "output.writeRawByte((byte) " + bytes[2] + ");\n";
                break;

            default:
                for (int i = 0; i < numBytes; i++) {
                    output += "output.writeRawByte((byte) " + bytes[i] + ");\n";
                }

        }
        return output;
    }

    protected void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        if (info.isFixedWidth() && info.isPacked()) {
            method.addNamedCode("" +
                    "final int dataSize = $fixedWidth:L * $field:N.length();\n" +
                    "size += $bytesPerTag:L + $protoSink:T.computeDelimitedSize(dataSize);\n", m);

        } else if (info.isFixedWidth() && info.isRepeated()) { // non packed
            method.addStatement(named("size += ($bytesPerTag:L + $fixedWidth:L) * $field:N.length()"));

        } else if (info.isPacked()) {
            method.addNamedCode("" +
                    "final int dataSize = $protoSink:T.computeRepeated$capitalizedType:LSizeNoTag($field:N);\n" +
                    "size += $bytesPerTag:L + $protoSink:T.computeDelimitedSize(dataSize);\n", m);

        } else if (info.isRepeated()) { // non packed
            method.addNamedCode("" +
                    "size += ($bytesPerTag:L * $field:N.length()) + $protoSink:T.computeRepeated$capitalizedType:LSizeNoTag($field:N);\n", m);

        } else if (info.isFixedWidth()) {
            method.addStatement("size += $L", (info.getBytesPerTag() + info.getFixedWidth())); // non-repeated

        } else {
            method.addStatement(named("size += $bytesPerTag:L + $protoSink:T.compute$capitalizedType:LSizeNoTag($field:N)")); // non-repeated
        }

    }

    protected void generateJsonSerializationCode(MethodSpec.Builder method) {
        if (info.isRepeated()) {
            method.addStatement(named("output.writeRepeated$capitalizedType:L($fieldNames:T.$field:N, $field:N)"));
        } else if (info.isEnum()) {
            method.addStatement(named("output.write$capitalizedType:L($fieldNames:T.$field:N, $field:N, $type:T.converter())"));
        } else {
            method.addStatement(named("output.write$capitalizedType:L($fieldNames:T.$field:N, $field:N)"));
        }
    }

    protected void generateJsonDeserializationCode(MethodSpec.Builder method) {
        method.addCode(clearOtherOneOfs).addCode(ensureFieldNotNull);
        if (info.isRepeated()) {
            if (info.isEnum()) {
                method.addStatement(named("input.readRepeated$capitalizedType:L($field:N, $type:T.converter())"));
            } else {
                method.addStatement(named("input.readRepeated$capitalizedType:L($field:N)"));
            }
            method.addStatement(named("$setHas:L"));
        } else if (info.isString() || info.isBytes() || info.isMessageOrGroup()) {
            method.addStatement(named("input.read$capitalizedType:L($field:N)"));
            method.addStatement(named("$setHas:L"));
        } else if (info.isPrimitive()) {
            method.addStatement(named("$field:N = input.read$capitalizedType:L()"));
            method.addStatement(named("$setHas:L"));
        } else if (info.isEnum()) {
            method.addStatement(named("final $protoEnum:T value = input.read$capitalizedType:L($type:T.converter())"))
                    .beginControlFlow("if (value != null)")
                    .addStatement(named("$field:N = value.getNumber()"))
                    .addStatement(named("$setHas:L"))
                    .nextControlFlow("else")
                    .addStatement("input.skipUnknownEnumValue()")
                    .endControlFlow();
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    protected void generateMemberMethods(TypeSpec.Builder type) {
        generateInitializedMethod(type);
        generateHasMethod(type);
        generateClearMethod(type);
        generateGetMethods(type);
        if (info.isEnum()) {
            generateExtraEnumAccessors(type);
        }
        if (info.isTryGetAccessorEnabled()) {
            generateTryGetMethod(type);
        }
        generateSetMethods(type);
    }

    protected void generateInitializedMethod(TypeSpec.Builder type) {
        if (info.isLazyAllocationEnabled()) {
            type.addMethod(MethodSpec.methodBuilder(info.getLazyInitName())
                    .addModifiers(Modifier.PRIVATE)
                    .addCode(CodeBlock.builder()
                            .beginControlFlow("if ($N == null)", info.getFieldName())
                            .add(named("$field:N = ")).addStatement(initializer())
                            .endControlFlow()
                            .build())
                    .build());
        }
    }

    private CodeBlock lazyFieldInit() {
        if (info.isLazyAllocationEnabled()) {
            return CodeBlock.builder()
                    .addStatement("$N()", info.getLazyInitName())
                    .build();
        } else {
            return EMPTY_BLOCK;
        }
    }

    protected void generateHasMethod(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getHazzerName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addStatement(named("return $getHas:L"))
                .build());
    }

    protected void generateSetMethods(TypeSpec.Builder type) {
        if (info.isRepeated() || info.isBytes()) {

            MethodSpec adder = MethodSpec.methodBuilder(info.getAdderName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getInputParameterType(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addCode(clearOtherOneOfs)
                    .addCode(ensureFieldNotNull)
                    .addStatement(named("$setHas:L"))
                    .addStatement(named("$field:N.add(value)"))
                    .addStatement(named("return this"))
                    .build();
            type.addMethod(adder);

            MethodSpec.Builder addAll = MethodSpec.methodBuilder("addAll" + info.getUpperName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ArrayTypeName.of(info.getInputParameterType()), "values", Modifier.FINAL)
                    .varargs(true)
                    .returns(info.getParentType())
                    .addCode(clearOtherOneOfs)
                    .addCode(ensureFieldNotNull)
                    .addStatement(named("$setHas:L"))
                    .addStatement(named("$field:N.addAll(values)"))
                    .addStatement(named("return this"));
            type.addMethod(addAll.build());

            if (info.isBytes()) {
                MethodSpec.Builder setBytes = MethodSpec.methodBuilder("set" + info.getUpperName())
                        .addAnnotations(info.getMethodAnnotations())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ArrayTypeName.of(info.getInputParameterType()), "values", Modifier.FINAL)
                        .varargs(true)
                        .returns(info.getParentType())
                        .addCode(clearOtherOneOfs)
                        .addCode(ensureFieldNotNull)
                        .addStatement(named("$setHas:L"))
                        .addStatement(named("$field:N.copyFrom(values)"))
                        .addStatement(named("return this"));
                type.addMethod(setBytes.build());
            }

        } else if (info.isMessageOrGroup() || info.isString()) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(info.getParentType())
                    .addParameter(info.getInputParameterType(), "value", Modifier.FINAL)
                    .addCode(clearOtherOneOfs)
                    .addCode(ensureFieldNotNull)
                    .addStatement(named("$setHas:L"))
                    .addStatement(named("$field:N.copyFrom(value)"))
                    .addStatement(named("return this"))
                    .build();
            type.addMethod(setter);

        } else if (info.isPrimitive() || info.isEnum()) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addCode(clearOtherOneOfs)
                    .addCode(ensureFieldNotNull)
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N = $valueOrNumber:L;\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(setter);

        }

    }

    /**
     * Enums are odd because they need to be converter back and forth and they
     * don't have the same type as the internal/repeated store. The normal
     * accessors provide access to the enum value, but for performance reasons
     * we also add accessors for the internal storage type that do not require
     * conversions.
     *
     * @param type
     */
    protected void generateExtraEnumAccessors(TypeSpec.Builder type) {
        if (!info.isEnum() || info.isRepeated())
            return;

        // Overload to get the internal store without conversion
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Value")
                .addAnnotations(info.getMethodAnnotations())
                .addJavadoc(named("" +
                        "Gets the value of the internal enum store. The result is\n" +
                        "equivalent to {@link $message:T#$getMethod:N()}.getNumber().\n"))
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addCode(enforceHasCheck)
                .addCode(ensureFieldNotNull)
                .addStatement(named("return $field:N"))
                .build());

        // Overload to set the internal value without conversion
        type.addMethod(MethodSpec.methodBuilder(info.getSetterName() + "Value")
                .addAnnotations(info.getMethodAnnotations())
                .addJavadoc(named("" +
                        "Sets the value of the internal enum store. This does not\n" +
                        "do any validity checks, so be sure to use appropriate value\n" +
                        "constants from {@link $type:T}. Setting an invalid value\n" +
                        "can cause {@link $message:T#$getMethod:N()} to return null\n"))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "value", Modifier.FINAL)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$field:N = value;\n" +
                        "return this;\n", m)
                .build());

    }

    protected void generateTryGetMethod(TypeSpec.Builder type) {
        MethodSpec.Builder tryGet = MethodSpec.methodBuilder(info.getTryGetName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getOptionalReturnType());

        tryGet.beginControlFlow(named("if ($hasMethod:N())"))
                .addStatement(named("return $optional:T.of($getMethod:N())"))
                .nextControlFlow("else")
                .addStatement(named("return $optional:T.empty()"))
                .endControlFlow();

        type.addMethod(tryGet.build());
    }

    protected void generateGetMethods(TypeSpec.Builder type) {
        MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .addCode(enforceHasCheck)
                .addCode(ensureFieldNotNull);

        if (info.isRepeated()) {
            getter.returns(storeType).addStatement(named("return $field:N"));
        } else if (info.isString()) {
            getter.returns(typeName).addStatement(named("return $field:N.getString()"));
        } else if (info.isEnum()) {
            if (info.hasDefaultValue()) {
                getter.returns(typeName).addStatement(named("return $type:T.forNumberOr($field:N, $defaultEnumValue:L)"));
            } else {
                getter.returns(typeName).addStatement(named("return $type:T.forNumber($field:N)"));
            }
        } else {
            getter.returns(typeName).addStatement(named("return $field:N"));
        }

        if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes()) {
            getter.addJavadoc(named("" +
                    "This method returns the internal storage object without modifying any has state.\n" +
                    "The returned object should not be modified and be treated as read-only.\n" +
                    "\n" +
                    "Use {@link #$getMutableMethod:N()} if you want to modify it.\n"));

            MethodSpec mutableGetter = MethodSpec.methodBuilder(info.getMutableGetterName())
                    .addJavadoc(named("" +
                            "This method returns the internal storage object and sets the corresponding\n" +
                            "has state. The returned object will become part of this message and its\n" +
                            "contents may be modified as long as the has state is not cleared.\n"))
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(storeType)
                    .addCode(clearOtherOneOfs)
                    .addCode(ensureFieldNotNull)
                    .addStatement(named("$setHas:L"))
                    .addStatement(named("return $field:N"))
                    .build();

            type.addMethod(getter.build());
            type.addMethod(mutableGetter);
        } else {
            type.addMethod(getter.build());
        }

        // Add an overload for Strings that let users get the backing Utf8Bytes
        if (!info.isRepeated() && info.isString()) {

            type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Bytes")
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(storeType)
                    .addCode(enforceHasCheck)
                    .addCode(ensureFieldNotNull)
                    .addStatement(named("return this.$field:N"))
                    .build());

            type.addMethod(MethodSpec.methodBuilder(info.getMutableGetterName() + "Bytes")
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(storeType)
                    .addCode(clearOtherOneOfs)
                    .addCode(ensureFieldNotNull)
                    .addStatement(named("$setHas:L"))
                    .addStatement(named("return this.$field:N"))
                    .build());

        }

    }

    protected void generateClearMethod(TypeSpec.Builder type) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(info.getClearName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addStatement(named("$clearHas:L"));
        generateClearCode(method);
        method.addStatement("return this");
        type.addMethod(method.build());
    }

    private CodeBlock generateClearOtherOneOfs() {
        if (!info.hasOtherOneOfFields())
            return EMPTY_BLOCK;

        return CodeBlock.builder()
                .addStatement("$N()", info.getClearOtherOneOfName())
                .build();
    }

    private CodeBlock generateEnforceHasCheck() {
        if (!info.isEnforceHasCheckEnabled())
            return EMPTY_BLOCK;

        return CodeBlock.builder()
                .beginControlFlow("if (!$N())", info.getHazzerName())
                .addStatement("throw new $T($S)", IllegalStateException.class,
                        "Field is not set. Check has state before accessing.")
                .endControlFlow()
                .build();
    }

    protected FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        typeName = info.getTypeName();
        storeType = info.getStoreType();

        // Common-variable map for named arguments
        m.put("field", info.getFieldName());
        m.put("default", info.getDefaultValue());
        if (info.isEnum()) {
          m.put("default", info.hasDefaultValue() ? info.getTypeName() + "." + info.getDefaultValue() + "_VALUE" : "0");
          m.put("defaultEnumValue", info.getTypeName() + "." + info.getDefaultValue());
          m.put("protoEnum", ParameterizedTypeName.get(RuntimeClasses.ProtoEnum, info.getTypeName()));
		} else {
          m.put("protoEnum", RuntimeClasses.ProtoEnum);
		}
        m.put("storeType", storeType);
        m.put("commentLine", info.getJavadoc());
        m.put("getMutableMethod", info.getMutableGetterName());
        m.put("lazyInitMethod", info.getLazyInitName());
        m.put("getMethod", info.getGetterName());
        m.put("setMethod", info.getSetterName());
        m.put("addMethod", info.getAdderName());
        m.put("hasMethod", info.getHazzerName());
        m.put("clearMethod", info.getClearName());
        m.put("getHas", info.getHasBit());
        m.put("setHas", info.getSetBit());
        m.put("clearHas", info.getClearBit());
        m.put("message", info.getParentType());
        m.put("type", typeName);
        m.put("number", info.getNumber());
        m.put("tag", info.getTag());
        m.put("capitalizedType", FieldUtil.getCapitalizedType(info.getDescriptor().getType()));
        m.put("secondArgs", info.isGroup() ? ", " + info.getNumber() : "");
        m.put("defaultField", info.getDefaultFieldName());
        m.put("bytesPerTag", info.getBytesPerTag());
        m.put("valueOrNumber", info.isEnum() ? "value.getNumber()" : "value");
        m.put("optional", info.getOptionalClass());
        if (info.isPackable()) m.put("packedTag", info.getPackedTag());
        if (info.isFixedWidth()) m.put("fixedWidth", info.getFixedWidth());
        if (info.isRepeated())
            m.put("getRepeatedIndex_i", info.isPrimitive() || info.isEnum() ? "array()[i]" : "get(i)");

        // utility classes
        m.put("fieldNames", getInfo().getParentTypeInfo().getFieldNamesClass());
        m.put("abstractMessage", RuntimeClasses.AbstractMessage);
        m.put("protoSource", RuntimeClasses.ProtoSource);
        m.put("protoSink", RuntimeClasses.ProtoSink);
        m.put("protoUtil", RuntimeClasses.ProtoUtil);

        // Common configuration-dependent code blocks
        clearOtherOneOfs = generateClearOtherOneOfs();
        enforceHasCheck = generateEnforceHasCheck();
        ensureFieldNotNull = lazyFieldInit();
    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;
    protected final TypeName storeType;
    protected final CodeBlock clearOtherOneOfs;
    protected final CodeBlock enforceHasCheck;
    protected final CodeBlock ensureFieldNotNull;
    private static final CodeBlock EMPTY_BLOCK = CodeBlock.builder().build();

    protected final HashMap<String, Object> m = new HashMap<>();

    private CodeBlock named(String format, Object... args /* does nothing, but makes IDE hints disappear */) {
        return CodeBlock.builder().addNamed(format, m).build();
    }

    private CodeBlock code(Consumer<CodeBlock.Builder> c) {
        CodeBlock.Builder block = CodeBlock.builder();
        c.accept(block);
        return block.build();
    }

}
