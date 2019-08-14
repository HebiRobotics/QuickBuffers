package us.hebi.robobuf.compiler;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.HashMap;

/**
 * This class generates all serialization logic and field related accessors.
 * It is a bit of a mess due to lots of switch statements, but I found that
 * splitting the types up similarly to how the protobuf-generator code is
 * organized makes it really difficult to find duplicate code or synergies
 * and was more difficult to work with overall.
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerator {

    public RequestInfo.FieldInfo getInfo() {
        return info;
    }

    public final void generateMemberFields(TypeSpec.Builder type) {
        FieldSpec.Builder field = FieldSpec.builder(storeType, info.getFieldName())
                .addJavadoc(info.getJavadoc())
                .addModifiers(Modifier.PRIVATE);

        if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes()) {
            field.addModifiers(Modifier.FINAL).initializer("new $T()", storeType);
        } else if (info.isString()) {
            field.addModifiers(Modifier.FINAL).initializer("new $T(0)", storeType);
        } else if (info.isPrimitive() || info.isEnum()) {
            // no initializer needed
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
        type.addField(field.build());

        if (info.isBytes() && info.hasDefaultValue()) {
            // byte[] default values are stored as utf8 strings, so we need to convert it first
            type.addField(FieldSpec.builder(ArrayTypeName.get(byte[].class), info.getDefaultFieldName())
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(CodeBlock.builder().addNamed("$internal:T.bytesDefaultValue(\"$default:L\")", m).build())
                    .build());
        }
    }

    public final void generateClearCode(MethodSpec.Builder method) {
        if (info.isRepeated() || info.isMessageOrGroup()) {
            method.addNamedCode("$field:N.clear();\n", m);

        } else if (info.isPrimitive() || info.isEnum()) {
            method.addNamedCode("$field:N = $default:L;\n", m);

        } else if (info.isString()) {
            method.addNamedCode("$field:N.setLength(0);\n", m);
            if (info.hasDefaultValue()) {
                method.addNamedCode("$field:N.append($default:S);\n", m);
            }

        } else if (info.isBytes()) {
            method.addNamedCode("$field:N.clear();\n", m);
            if (info.hasDefaultValue())
                method.addNamedCode("$field:N.copyFrom($defaultField:N);\n", m);

        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    public final void generateCopyFromCode(MethodSpec.Builder method) {
        if (info.isRepeated() || info.isBytes() || info.isMessageOrGroup()) {
            method.addNamedCode("$field:N.copyFrom(other.$field:N);\n", m);

        } else if (info.isPrimitive() || info.isEnum()) {
            method.addNamedCode("$field:N = other.$field:N;\n", m);

        } else if (info.isString()) {
            method.addNamedCode("$field:N.setLength(0);\n", m);
            method.addNamedCode("$field:N.append(other.$field:N);\n", m);

        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    public final void generateEqualsStatement(MethodSpec.Builder method) {
        if (info.isRepeated() || info.isBytes() || info.isMessageOrGroup()) {
            method.addNamedCode("$field:N.equals(other.$field:N)", m);

        } else if (info.isString()) {
            method.addNamedCode("$roboUtil:T.equals($field:N, other.$field:N)", m);

        } else if (typeName == TypeName.DOUBLE) {
            method.addNamedCode("Double.doubleToLongBits($field:N) == Double.doubleToLongBits(other.$field:N)", m);

        } else if (typeName == TypeName.FLOAT) {
            method.addNamedCode("Float.floatToIntBits($field:N) == Float.floatToIntBits(other.$field:N)", m);

        } else if (info.isPrimitive() || info.isEnum()) {
            method.addNamedCode("$field:N == other.$field:N", m);

        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    public final void generateMergingCode(MethodSpec.Builder method) {
        // non-packed fields still expected to be located together. Most repeated primitives should be
        // packed, so optimizing this further is not a priority.
        if (info.isRepeated() && (info.isPrimitive() || info.isEnum())) {
            method
                    .addNamedCode("final int arrayLength = $wireFormat:T.getRepeatedFieldArrayLength(input, $tag:L);\n", m)
                    .addNamedCode("$field:N.requestSize(arrayLength);\n", m)
                    .beginControlFlow("for (int i = 0; i < arrayLength - 1; i++)")
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .addStatement("input.readTag()")
                    .endControlFlow()
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .addNamedCode("$setHas:L;\n", m);

        } else if (info.isRepeated()) {
            method
                    .addNamedCode("final int arrayLength = $wireFormat:T.getRepeatedFieldArrayLength(input, $tag:L);\n", m)
                    .addNamedCode("$field:N.requestSize(arrayLength);\n", m)
                    .beginControlFlow("for (int i = 0; i < arrayLength - 1; i++)")
                    .addNamedCode("input.read$capitalizedType:L($field:N.getAndAdd()$secondArgs:L);\n", m)
                    .addStatement("input.readTag()")
                    .endControlFlow()
                    .addNamedCode("input.read$capitalizedType:L($field:N.getAndAdd()$secondArgs:L);\n", m)
                    .addNamedCode("$setHas:L;\n", m);

        } else if (info.isString() || info.isMessageOrGroup() || info.isBytes()) {
            method
                    .addNamedCode("input.read$capitalizedType:L($field:N$secondArgs:L);\n", m)
                    .addNamedCode("$setHas:L;\n", m);

        } else if (info.isPrimitive()) {
            method
                    .addNamedCode("$field:N = input.read$capitalizedType:L();\n", m)
                    .addNamedCode("$setHas:L;\n", m);

        } else if (info.isEnum()) {
            method
                    .addStatement("final int value = input.readInt32()")
                    .beginControlFlow("if ($T.forNumber(value) != null)", typeName)
                    .addNamedCode("$field:N = value;\n", m)
                    .addNamedCode("$setHas:L;\n", m)
                    .endControlFlow();

        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    public final void generateMergingCodeFromPacked(MethodSpec.Builder method) {
        if (info.isFixedWidth()) {

            // For fixed width types we can copy the raw memory
            method.addNamedCode("input.readPacked$capitalizedType:L($field:N);\n", m);
            method.addNamedCode("$setHas:L;\n", m);

        } else if (info.isPrimitive() || info.isEnum()) {

            // We don't know how many items there actually are, so we need to
            // look-ahead once we run out of space in the backing store.
            method
                    .addStatement("final int length = input.readRawVarint32()")
                    .addStatement("final int limit = input.pushLimit(length)")
                    .beginControlFlow("while (input.getBytesUntilLimit() > 0)")

                    // Defer count-checks until we run out of capacity
                    .addComment("do a look-ahead to avoid unnecessary allocations")
                    .beginControlFlow("if ($N.remainingCapacity() == 0)", info.getFieldName())
                    .addStatement("final int position = input.getPosition()")
                    .addStatement("int numEntries = 0")
                    .beginControlFlow("while (input.getBytesUntilLimit() > 0)")
                    .addNamedCode("input.read$capitalizedType:L();\n", m)
                    .addStatement("numEntries++")
                    .endControlFlow()
                    .addStatement("input.rewindToPosition(position)")
                    .addNamedCode("$field:N.requestSize(numEntries);\n", m)
                    .endControlFlow()

                    // Add data
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .endControlFlow()

                    .addStatement("input.popLimit(limit)")
                    .addNamedCode("$setHas:L;\n", m);

        } else {
            // Only primitives and enums can be packed
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
    }

    public final void generateSerializationCode(MethodSpec.Builder method) {
        if (info.isPacked() && info.isFixedWidth()) {
            method.addNamedCode("output.writePacked$capitalizedType:L($number:L, $field:N);\n", m);

        } else if (info.isPacked()) {
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $computeClass:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "output.writeRawVarint32($packedTag:L);\n" +
                    "output.writeRawVarint32(dataSize);\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "output.write$capitalizedType:LNoTag($field:N.get(i));\n" +
                    "$<}\n", m);

        } else if (info.isRepeated()) {
            method.addNamedCode("" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "output.write$capitalizedType:L($number:L, $field:N.get(i));\n" +
                    "$<}\n", m);

        } else {
            method.addNamedCode("output.write$capitalizedType:L($number:L, $field:N);\n", m); // non-repeated
        }
    }

    public final void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        if (info.isPacked() && info.isFixedWidth()) {
            method.addNamedCode("" +
                    "final int dataSize = $fixedWidth:L * $field:N.length();\n" +
                    "size += dataSize;\n" +
                    "size += $bytesPerTag:L;\n" +
                    "size += $computeClass:T.computeRawVarint32Size(dataSize);\n", m);

        } else if (info.isPacked()) {
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $computeClass:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "size += dataSize;\n" +
                    "size += $bytesPerTag:L;\n" +
                    "size += $computeClass:T.computeRawVarint32Size(dataSize);\n", m);

        } else if (info.isRepeated()) { // non packed
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $computeClass:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "size += dataSize;\n" +
                    "size += $bytesPerTag:L * $field:N.length();\n", m);

        } else {
            method.addNamedCode("size += $computeClass:T.compute$capitalizedType:LSize($number:L, $field:N);\n", m); // non-repeated
        }

    }

    public final void generateMemberMethods(TypeSpec.Builder type) {
        generateHasMethod(type);
        if (info.isEnum()) {
            generateGetEnumMethods(type);
        } else {
            generateGetMethods(type);
        }
        generateSetMethods(type);
        generateClearMethod(type);
    }


    private void generateHasMethod(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getHazzerName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addNamedCode("return $getHas:L;\n", m)
                .build());
    }

    private void generateSetMethods(TypeSpec.Builder type) {
        if (info.isRepeated() || info.isBytes()) {

            MethodSpec adder = MethodSpec.methodBuilder(info.getAdderName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getInputParameterType(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.add($valueOrNumber:L);\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(adder);

            MethodSpec.Builder addAll = MethodSpec.methodBuilder("addAll" + info.getUpperName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ArrayTypeName.of(info.getInputParameterType()), "values", Modifier.FINAL)
                    .varargs(true)
                    .returns(info.getParentType())
                    .addNamedCode("$setHas:L;\n", m);
            if (!info.isEnum()) {
                addAll.addNamedCode("$field:N.addAll(values);\n", m);
            } else {
                addAll.addNamedCode("" +
                        "$field:N.requestSize(values.length);\n" +
                        "for ($type:T value : values) {$>\n" +
                        "$field:N.add($valueOrNumber:L);\n" +
                        "$<}\n", m);
            }
            addAll.addStatement("return this");
            type.addMethod(addAll.build());

        } else if (info.isMessageOrGroup()) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(info.getParentType())
                    .addParameter(typeName, "value", Modifier.FINAL)
                    .addNamedCode("$setHas:L;\n", m)
                    .addNamedCode("$field:N.copyFrom(value);\n", m)
                    .addStatement("return this")
                    .build();
            type.addMethod(setter);

        } else if (info.isString()) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getInputParameterType(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.setLength(0);\n" +
                            "$field:N.append(value);\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(setter);

        } else if (info.isPrimitive() || info.isEnum()) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N = $valueOrNumber:L;\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(setter);

        }

    }

    private void generateGetEnumMethods(TypeSpec.Builder type) {
        // Enums are weird because they need to be converter back and forth
        // and we can't just expose the repeated store. Maybe we should switch
        // to using either fully int or fully enum.
        MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);

        if (!info.isRepeated()) { // get()
            if (!info.hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N);\n", m);
            } else {
                getter.addNamedCode("" +
                        "final $type:T result = $type:T.forNumber($field:N);\n" +
                        "return result == null ? $defaultEnumValue:L : result;\n", m);
            }

        } else { // getCount() & get(index)
            getter.addParameter(int.class, "index", Modifier.FINAL);
            if (!info.hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N.get(index));\n", m);
            } else {
                getter.addNamedCode("" +
                        "final $type:T result = $type:T.forNumber($field:N.get(index));\n" +
                        "return result == null ? $defaultEnumValue:L : result;\n", m);
            }

            MethodSpec getCount = MethodSpec.methodBuilder(info.getGetterName() + "Count")
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(int.class)
                    .addNamedCode("return $field:N.length();\n", m)
                    .build();
            type.addMethod(getCount);
        }
        type.addMethod(getter.build());
    }

    protected void generateGetMethods(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(storeType)
                .addNamedCode("return $field:N;\n", m)
                .build());

        if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes() || info.isString()) {
            MethodSpec mutableGetter = MethodSpec.methodBuilder(info.getMutableGetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(storeType)
                    .addNamedCode("$setHas:L;\n", m)
                    .addNamedCode("return $field:N;\n", m)
                    .build();
            type.addMethod(mutableGetter);
        }
    }

    private void generateClearMethod(TypeSpec.Builder type) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(info.getClearName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("$clearHas:L;\n", m);
        generateClearCode(method);
        method.addStatement("return this");
        type.addMethod(method.build());
    }

    public FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        typeName = info.getTypeName();
        storeType = info.getStoreType();

        // Common-variable map for named arguments
        m.put("field", info.getFieldName());
        m.put("default", info.getDefaultValue());
        if (info.isEnum()) {
            m.put("default", info.hasDefaultValue() ? info.getTypeName() + "." + info.getDefaultValue() + ".getNumber()" : "0");
            m.put("defaultEnumValue", info.getTypeName() + "." + info.getDefaultValue());
        }
        m.put("hasMethod", info.getHazzerName());
        m.put("setMethod", info.getSetterName());
        m.put("addMethod", info.getAdderName());
        m.put("getHas", info.getHasBit());
        m.put("setHas", info.getSetBit());
        m.put("clearHas", info.getClearBit());
        m.put("message", info.getParentType());
        m.put("type", typeName);
        m.put("number", info.getNumber());
        m.put("tag", info.getTag());
        m.put("capitalizedType", RuntimeClasses.getCapitalizedType(info.getDescriptor().getType()));
        m.put("computeClass", RuntimeClasses.PROTO_DEST);
        m.put("roboUtil", RuntimeClasses.ROBO_UTIL);
        m.put("wireFormat", RuntimeClasses.WIRE_FORMAT);
        m.put("internal", RuntimeClasses.INTERNAL);
        m.put("secondArgs", info.isGroup() ? ", " + info.getNumber() : "");
        m.put("defaultField", info.getDefaultFieldName());
        m.put("bytesPerTag", info.getBytesPerTag());
        m.put("valueOrNumber", info.isEnum() ? "value.getNumber()" : "value");
        if (info.isPackable()) m.put("packedTag", info.getPackedTag());
        if (info.isFixedWidth()) m.put("fixedWidth", info.getFixedWidth());

    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;
    protected final TypeName storeType;

    protected final HashMap<String, Object> m = new HashMap<>();

}
