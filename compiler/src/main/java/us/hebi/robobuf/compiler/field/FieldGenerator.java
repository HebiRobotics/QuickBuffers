package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.*;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;
import java.util.HashMap;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public abstract class FieldGenerator {

    public RequestInfo.FieldInfo getInfo() {
        return info;
    }

    public final void generateMemberFields(TypeSpec.Builder type) {
        FieldSpec.Builder value = FieldSpec.builder(storeType, info.getFieldName())
                .addJavadoc(info.getJavadoc())
                .addModifiers(Modifier.PRIVATE);

        if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes()) {
            value.addModifiers(Modifier.FINAL).initializer("new $T()", storeType);
        } else if (info.isString()) {
            value.addModifiers(Modifier.FINAL).initializer("new $T(0)", storeType);
        } else if (info.isPrimitive() || info.isEnum()) {
            // do nothing
        } else {
            throw new IllegalStateException("unhandled field: " + info.getDescriptor());
        }
        type.addField(value.build());

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

        } else if (fieldType == TypeName.DOUBLE) {
            method.addNamedCode("Double.doubleToLongBits($field:N) == Double.doubleToLongBits(other.$field:N)", m);

        } else if (fieldType == TypeName.FLOAT) {
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
                    .addNamedCode("$field:N.ensureSpace(arrayLength);\n", m)
                    .beginControlFlow("for (int i = 0; i < arrayLength - 1; i++)")
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .addStatement("input.readTag()")
                    .endControlFlow()
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .addNamedCode("$setHas:L;\n", m);

        } else if (info.isRepeated()) {
            method
                    .addNamedCode("final int arrayLength = $wireFormat:T.getRepeatedFieldArrayLength(input, $tag:L);\n", m)
                    .addNamedCode("$field:N.ensureSpace(arrayLength);\n", m)
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
                    .beginControlFlow("if ($T.forNumber(value) != null)", fieldType)
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
                    .addNamedCode("$field:N.ensureSpace(numEntries);\n", m)
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
            method.addNamedCode("output.write$capitalizedType:L($number:L, $serializableValue:L);\n", m); // non-repeated
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
            method.addNamedCode("size += $computeClass:T.compute$capitalizedType:LSize($number:L, $serializableValue:L);\n", m); // non-repeated
        }

    }

    protected abstract void generateSetter(TypeSpec.Builder type);

    public void generateMemberMethods(TypeSpec.Builder type) {
        generateHazzer(type);
        generateGetter(type);
        generateSetter(type);
        generateClearer(type);
    }

    protected void generateHazzer(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getHazzerName())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addNamedCode("return $getHas:L;\n", m)
                .build());
    }

    protected void generateGetter(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addNamedCode("return $field:N;\n", m)
                .build());

        if (info.isMutableReferenceObject()) {
            MethodSpec mutableGetter = MethodSpec.methodBuilder(info.getMutableGetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(fieldType)
                    .addNamedCode("$setHas:L;\n", m)
                    .addNamedCode("return $field:N;\n", m)
                    .build();
            type.addMethod(mutableGetter);
        }
    }

    protected void generateClearer(TypeSpec.Builder type) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(info.getClearName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("$clearHas:L;\n", m);
        generateClearCode(method);
        method.addStatement("return this");
        type.addMethod(method.build());
    }

    protected FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        fieldType = info.getTypeName();
        storeType = info.getStoreType();

        // Common-variable map for named arguments
        m.put("field", info.getFieldName());
        m.put("default", info.getDefaultValue());
        m.put("hasMethod", info.getHazzerName());
        m.put("setMethod", info.getSetterName());
        m.put("addMethod", info.getAdderName());
        m.put("getHas", info.getHasBit());
        m.put("setHas", info.getSetBit());
        m.put("clearHas", info.getClearBit());
        m.put("message", info.getParentType());
        m.put("type", fieldType);
        m.put("number", info.getNumber());
        m.put("tag", info.getTag());
        m.put("capitalizedType", RuntimeClasses.getCapitalizedType(info.getDescriptor().getType()));
        m.put("serializableValue", info.getFieldName());
        m.put("computeClass", RuntimeClasses.PROTO_DEST);
        m.put("roboUtil", RuntimeClasses.ROBO_UTIL);
        m.put("wireFormat", RuntimeClasses.WIRE_FORMAT);
        m.put("internal", RuntimeClasses.INTERNAL);
        m.put("secondArgs", info.isGroup() ? ", " + info.getNumber() : "");
        m.put("defaultField", info.getDefaultFieldName());
        m.put("bytesPerTag", info.getBytesPerTag());
        if (info.isPackable()) m.put("packedTag", info.getPackedTag());
        if (info.isFixedWidth()) m.put("fixedWidth", info.getFixedWidth());

    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName fieldType;
    protected final TypeName storeType;

    protected final HashMap<String, Object> m = new HashMap<>();

}
