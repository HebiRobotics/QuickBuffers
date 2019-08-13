package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
class PrimitiveField {

    static class OptionalPrimitiveField extends FieldGenerator {

        OptionalPrimitiveField(RequestInfo.FieldInfo info) {
            super(info);
        }

        @Override
        public void generateMemberFields(TypeSpec.Builder type) {
            FieldSpec value = FieldSpec.builder(typeName, info.getFieldName())
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            type.addField(value);
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N = value;\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(setter);
        }

        @Override
        public void generateEqualsStatement(MethodSpec.Builder method) {
            if (typeName == TypeName.FLOAT) {
                method.addNamedCode("Float.floatToIntBits($field:N) == Float.floatToIntBits(other.$field:N)", m);
            } else if (typeName == TypeName.DOUBLE) {
                method.addNamedCode("Double.doubleToLongBits($field:N) == Double.doubleToLongBits(other.$field:N)", m);
            } else {
                method.addNamedCode("$field:N == other.$field:N", m);
            }
        }

    }

    static class RepeatedPrimitiveField extends RepeatedField {

        RepeatedPrimitiveField(RequestInfo.FieldInfo info) {
            super(info);
        }

        @Override
        public void generateMergingCode(MethodSpec.Builder method) {
            // non-packed fields still expected to be located together. Most repeated primitives should be
            // packed, so optimizing this further is not a priority.
            method
                    .addNamedCode("final int arrayLength = $wireFormat:T.getRepeatedFieldArrayLength(input, $tag:L);\n", m)
                    .addNamedCode("$field:N.ensureSpace(arrayLength);\n", m)
                    .beginControlFlow("for (int i = 0; i < arrayLength - 1; i++)")
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .addStatement("input.readTag()")
                    .endControlFlow()
                    .addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m)
                    .addNamedCode("$setHas:L;\n", m);
        }

        @Override
        public void generateMergingCodeFromPacked(MethodSpec.Builder method) {

            if (info.isFixedWidth()) {

                // For fixed width types we can potentially just copy the raw memory
                method.addNamedCode("input.readPacked$capitalizedType:L($field:N);\n", m);
                method.addNamedCode("$setHas:L;\n", m);

            } else {

                // Varint decoding. In steady state we should already have an array that
                // can fit the incoming data, so we can skip the array size checks
                method.addNamedCode("" +
                        "final int length = input.readRawVarint32();\n" +
                        "final int limit = input.pushLimit(length);\n" +
                        "\n" +

                        "// Do a size check if the data is guaranteed not to fit\n" +
                        "if ($field:N.remainingCapacity() < length) {$>\n" +
                        "int arrayLength = 0;\n" +
                        "int startPos = input.getPosition();\n" +

                        "while (input.getBytesUntilLimit() > 0) {$>\n" +
                        "input.read$capitalizedType:L();\n" +
                        "arrayLength++;\n" +
                        "$<}\n" +

                        "input.rewindToPosition(startPos);\n" +
                        "$field:N.ensureSpace(arrayLength);\n" +
                        "$<}\n" +
                        "\n" +

                        "// Fill in data\n" +
                        "while (input.getBytesUntilLimit() > 0) {$>\n" +
                        "$field:N.add(input.read$capitalizedType:L());\n" +
                        "$<}\n" +

                        "input.popLimit(limit);\n" +
                        "$setHas:L;\n", m);

            }

        }

        @Override
        public void generateSerializationCode(MethodSpec.Builder method) {
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

            } else {
                super.generateSerializationCode(method);
            }
        }

        @Override
        public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
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

            } else {
                super.generateComputeSerializedSizeCode(method);
            }
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.set(index, value);\n" +
                            "return this;\n", m)
                    .build());

            type.addMethod(MethodSpec.methodBuilder("add" + info.getUpperName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.add(value);\n" +
                            "return this;\n", m)
                    .build());
        }

    }
}
