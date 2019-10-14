package us.hebi.robobuf.compiler;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.HashMap;

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
                .addJavadoc(info.getJavadoc())
                .addModifiers(Modifier.PRIVATE);

        if (info.isRepeated() && info.isMessageOrGroup()) {
            field.addModifiers(Modifier.FINAL).initializer("new $T($T.getFactory())", storeType, info.getTypeName());
        } else if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes()) {
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
                    .initializer(CodeBlock.builder().addNamed("$internalUtil:T.bytesDefaultValue(\"$default:L\")", m).build())
                    .build());
        }
    }

    protected void generateClearCode(MethodSpec.Builder method) {
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

    protected void generateCopyFromCode(MethodSpec.Builder method) {
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

    protected void generateEqualsStatement(MethodSpec.Builder method) {
        if (info.isRepeated() || info.isBytes() || info.isMessageOrGroup()) {
            method.addNamedCode("$field:N.equals(other.$field:N)", m);

        } else if (info.isString()) {
            method.addNamedCode("$internalUtil:T.equals($field:N, other.$field:N)", m);

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

    protected void generateMergingCode(MethodSpec.Builder method) {
        if (info.isRepeated()) {
            method
                    .addStatement("int nextTagPosition")
                    .addNamedCode("do {$>\n" +
                            "// look ahead for more items so we resize only once\n" +
                            "if ($field:N.remainingCapacity() == 0) {$>\n" +
                            "int count = $internalUtil:T.getRepeatedFieldArrayLength(input, $tag:L);\n" +
                            "$field:N.requireCapacity(count);\n" +
                            "$<}\n", m)
                    .addNamedCode(info.isPrimitive() || info.isEnum() ?
                            "$field:N.add(input.read$capitalizedType:L());\n"
                            : "input.read$capitalizedType:L($field:N.getAndAdd()$secondArgs:L);\n", m)
                    .addNamedCode("" +
                            "nextTagPosition = input.getPosition();\n" +
                            "$<} while (input.readTag() == $tag:L);\n" +
                            "input.rewindToPosition(nextTagPosition);\n", m)
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

    protected void generateMergingCodeFromPacked(MethodSpec.Builder method) {
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
                    .addComment("look ahead for more items so we resize only once")
                    .beginControlFlow("if ($N.remainingCapacity() == 0)", info.getFieldName())
                    .addStatement("final int position = input.getPosition()")
                    .addStatement("int count = 0")
                    .beginControlFlow("while (input.getBytesUntilLimit() > 0)")
                    .addNamedCode("input.read$capitalizedType:L();\n", m)
                    .addStatement("count++")
                    .endControlFlow()
                    .addStatement("input.rewindToPosition(position)")
                    .addNamedCode("$field:N.requireCapacity(count);\n", m)
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

    protected void generateSerializationCode(MethodSpec.Builder method) {
        m.put("writeTagToOutput", generateWriteVarint32(getInfo().getTag()));
        if (info.isPacked()) {
            m.put("writePackedTagToOutput", generateWriteVarint32(getInfo().getPackedTag()));
        }

        if (info.isPacked() && info.isFixedWidth()) {
            method.addNamedCode("" +
                    "$writePackedTagToOutput:L;\n" +
                    "output.writePacked$capitalizedType:LNoTag($field:N);\n", m);

        } else if (info.isPacked()) {
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $computeClass:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "$writePackedTagToOutput:L;\n" +
                    "output.writeRawVarint32(dataSize);\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "output.write$capitalizedType:LNoTag($field:N.get(i));\n" +
                    "$<}\n", m);

        } else if (info.isRepeated()) {
            method.addNamedCode("" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "$writeTagToOutput:L;\n" +
                    "output.write$capitalizedType:LNoTag($field:N.get(i));\n" +
                    "$<}\n", m);

        } else {
            // unroll varint tag loop
            method.addNamedCode("$writeTagToOutput:L;\n", m);
            method.addNamedCode("output.write$capitalizedType:LNoTag($field:N);\n", m); // non-repeated
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

            case 5:
                value = (bytes[3] << 24 | bytes[2] << 16 | bytes[1] << 8 | bytes[0]);
                output += "output.writeRawLittleEndian32(" + value + ");\n";
                output += "output.writeRawByte((byte) " + bytes[4] + ")";
                break;

            case 4:
                value = (bytes[3] << 24 | bytes[2] << 16 | bytes[1] << 8 | bytes[0]);
                output += "output.writeRawLittleEndian32(" + value + ")";
                break;

            case 3:
                value = (bytes[1] << 8 | bytes[0]);
                output += "output.writeRawLittleEndian16((short) " + value + ");\n";
                output += "output.writeRawByte((byte) " + bytes[2] + ")";
                break;

            case 2:
                value = (bytes[1] << 8 | bytes[0]);
                output += "output.writeRawLittleEndian16((short) " + value + ")";
                break;

            default:
                for (int i = 0; i < numBytes - 1; i++) {
                    output += "output.writeRawByte((byte) " + bytes[i] + ");\n";
                }
                output += "output.writeRawByte((byte) " + bytes[numBytes - 1] + ")";
        }

        return output;
    }

    protected void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
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

    protected void generateMemberMethods(TypeSpec.Builder type) {
        generateHasMethod(type);
        if (info.isEnum()) {
            generateGetEnumMethods(type);
        } else {
            generateGetMethods(type);
        }
        generateSetMethods(type);
        generateClearMethod(type);
    }


    protected void generateHasMethod(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getHazzerName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addNamedCode("return $getHas:L;\n", m)
                .build());
    }

    protected void generateSetMethods(TypeSpec.Builder type) {
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
                        "$field:N.requireCapacity(values.length);\n" +
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

    protected void generateGetEnumMethods(TypeSpec.Builder type) {
        // Enums are odd because they need to be converter back and forth and they
        // don't have the same type as the repeated store. Maybe we should switch
        // to using either fully int or fully enum?
        MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);

        if (!info.isRepeated()) { // get()
            if (!info.hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N);\n", m);
            } else {
                getter.addNamedCode("return $type:T.forNumberOr($field:N, $defaultEnumValue:L);\n", m);
            }

        } else { // getCount() & get(index)
            getter.addParameter(int.class, "index", Modifier.FINAL);
            getter.addNamedCode("return $type:T.forNumber($field:N.get(index));\n", m);

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

    protected void generateClearMethod(TypeSpec.Builder type) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(info.getClearName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("$clearHas:L;\n", m);
        generateClearCode(method);
        method.addStatement("return this");
        type.addMethod(method.build());
    }

    protected FieldGenerator(RequestInfo.FieldInfo info) {
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
        m.put("capitalizedType", FieldUtil.getCapitalizedType(info.getDescriptor().getType()));
        m.put("computeClass", RuntimeClasses.ProtoSink);
        m.put("internalUtil", RuntimeClasses.InternalUtil);
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
