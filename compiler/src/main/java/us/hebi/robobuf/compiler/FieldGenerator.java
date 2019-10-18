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
                .addJavadoc(named("$commentLine:L"))
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
                    .initializer(named("$abstractMessage:T.bytesDefaultValue(\"$default:L\")"))
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

    protected void generateClearQuickCode(MethodSpec.Builder method) {
        if (info.isRepeated() || info.isBytes()) {
            method.addNamedCode("$field:N.clear();\n", m);
        } else if (info.isMessageOrGroup()) {
            method.addNamedCode("$field:N.clearQuick();\n", m);
        } else if (info.isString()) {
            method.addNamedCode("$field:N.setLength(0);\n", m);
        } else if (info.isPrimitive() || info.isEnum()) {
            // do nothing
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
            method.addNamedCode("$protoUtil:T.isEqual($field:N, other.$field:N)", m);

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
                            "int count = $protoSource:T.getRepeatedFieldArrayLength(input, $tag:L);\n" +
                            "$field:N.reserve(count);\n" +
                            "$<}\n", m)
                    .addNamedCode(info.isPrimitive() || info.isEnum() ?
                            "$field:N.add(input.read$capitalizedType:L());\n"
                            : "input.read$capitalizedType:L($field:N.next()$secondArgs:L);\n", m)
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
                    .addNamedCode("$field:N.reserve(count);\n", m)
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
                    "$writePackedTagToOutput:L" +
                    "output.writePacked$capitalizedType:LNoTag($field:N);\n", m);

        } else if (info.isPacked()) {
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $protoSink:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "$writePackedTagToOutput:L" +
                    "output.writeRawVarint32(dataSize);\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "output.write$capitalizedType:LNoTag($field:N.get(i));\n" +
                    "$<}\n", m);

        } else if (info.isRepeated()) {
            method.addNamedCode("" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "$writeTagToOutput:L" +
                    "output.write$capitalizedType:LNoTag($field:N.get(i));\n" +
                    "$<}\n", m);

        } else {
            // unroll varint tag loop
            method.addNamedCode("$writeTagToOutput:L", m);
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
        if (info.isPacked() && info.isFixedWidth()) {
            method.addNamedCode("" +
                    "final int dataSize = $fixedWidth:L * $field:N.length();\n" +
                    "size += dataSize;\n" +
                    "size += $bytesPerTag:L;\n" +
                    "size += $protoSink:T.computeRawVarint32Size(dataSize);\n", m);

        } else if (info.isPacked()) {
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $protoSink:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "size += dataSize;\n" +
                    "size += $bytesPerTag:L;\n" +
                    "size += $protoSink:T.computeRawVarint32Size(dataSize);\n", m);

        } else if (info.isRepeated()) { // non packed
            method.addNamedCode("" +
                    "int dataSize = 0;\n" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "dataSize += $protoSink:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                    "$<}\n" +
                    "size += dataSize;\n" +
                    "size += $bytesPerTag:L * $field:N.length();\n", m);

        } else if (info.isFixedWidth()) {
            method.addStatement("size += $L", (info.getBytesPerTag() + info.getFixedWidth())); // non-repeated

        } else {
            method.addNamedCode("size += $bytesPerTag:L + $protoSink:T.compute$capitalizedType:LSizeNoTag($field:N);\n", m); // non-repeated
        }

    }

    protected void generateMemberMethods(TypeSpec.Builder type) {
        generateHasMethod(type);
        if (info.isEnum()) {
            generateExtraEnumAccessors(type);
        }
        generateGetMethods(type);
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
                        "$field:N.reserve(values.length);\n" +
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

    /**
     * Enums are odd because they need to be converter back and forth and they
     * don't have the same type as the internal/repeated store. In this case we give
     * raw access to the int number, but also add convenience wrappers to get
     * the enum value.
     *
     * @param type
     */
    protected void generateExtraEnumAccessors(TypeSpec.Builder type) {

        MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);

        if (!info.isRepeated()) {

            // getField() that maps from int to enum
            if (!info.hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N);\n", m);
            } else {
                getter.addNamedCode("return $type:T.forNumberOr($field:N, $defaultEnumValue:L);\n", m);
            }

            // We also want an overload to set the internal value directly.
            // For repeated enums people can just use the mutable store.
            MethodSpec setNumber = MethodSpec.methodBuilder(info.getSetterName())
                    .addAnnotations(info.getMethodAnnotations())
                    .addJavadoc(named("" +
                            "Sets the value of the internal enum store. This does not\n" +
                            "do any validity checks, so be sure to use appropriate value\n" +
                            "constants from {@link $type:T}.\n"))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getStoreType(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N = value;\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(setNumber);

        } else {

            // getCount() & get(index)
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
        // We are adding another getter for enums with the same name
        String getterName = info.getGetterName();
        if (info.isEnum() && !info.isRepeated()) {
            getterName += "Number";
        }

        MethodSpec.Builder getter = MethodSpec.methodBuilder(getterName)
                .addAnnotations(info.getMethodAnnotations())
                .addModifiers(Modifier.PUBLIC)
                .returns(storeType)
                .addNamedCode("return $field:N;\n", m);

        if (info.isEnum()) {
            getter.addJavadoc(named("" +
                    "Gets the value of the internal enum store. The result is\n" +
                    "equivalent to {@link #$getMethod:N()}.getNumber().\n"));
        }

        if (info.isRepeated() || info.isMessageOrGroup() || info.isBytes() || info.isString()) {
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
                    .addNamedCode("$setHas:L;\n", m)
                    .addNamedCode("return $field:N;\n", m)
                    .build();

            type.addMethod(getter.build());
            type.addMethod(mutableGetter);
        } else {
            type.addMethod(getter.build());
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
            m.put("default", info.hasDefaultValue() ? info.getTypeName() + "." + info.getDefaultValue() + "_VALUE" : "0");
            m.put("defaultEnumValue", info.getTypeName() + "." + info.getDefaultValue());
        }
        m.put("commentLine", info.getJavadoc());
        m.put("getMutableMethod", info.getMutableGetterName());
        m.put("getMethod", info.getGetterName());
        m.put("setMethod", info.getSetterName());
        m.put("addMethod", info.getAdderName());
        m.put("hasMethod", info.getHazzerName());
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
        if (info.isPackable()) m.put("packedTag", info.getPackedTag());
        if (info.isFixedWidth()) m.put("fixedWidth", info.getFixedWidth());

        // utility classes
        m.put("abstractMessage", RuntimeClasses.AbstractMessage);
        m.put("protoSource", RuntimeClasses.ProtoSource);
        m.put("protoSink", RuntimeClasses.ProtoSink);
        m.put("protoUtil", RuntimeClasses.ProtoUtil);
    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;
    protected final TypeName storeType;

    protected final HashMap<String, Object> m = new HashMap<>();

    private CodeBlock named(String format, Object... args /* does nothing, but makes IDE hints disappear */) {
        return CodeBlock.builder().addNamed(format, m).build();
    }

}
