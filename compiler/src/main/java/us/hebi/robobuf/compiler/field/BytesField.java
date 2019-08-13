package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.*;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 12 Aug 2019
 */
class BytesField {

    private static final TypeName BYTE_ARRAY = ArrayTypeName.get(byte[].class);

    static class OptionalBytesField extends FieldGenerator {

        OptionalBytesField(RequestInfo.FieldInfo info) {
            super(info);
            m.put("setMethod", info.getSetterName());
            m.put("defaultField", getDefaultField());
        }

        private String getDefaultField() {
            return "_default" + info.getUpperName();
        }

        @Override
        public void generateMemberFields(TypeSpec.Builder type) {
            type.addField(FieldSpec.builder(RuntimeClasses.BYTES_STORAGE_CLASS, info.getFieldName())
                    .addJavadoc(info.getJavadoc())
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T()", RuntimeClasses.BYTES_STORAGE_CLASS)
                    .build());

            if (!info.getDefaultValue().isEmpty()) {
                type.addField(FieldSpec.builder(BYTE_ARRAY, getDefaultField(), Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(CodeBlock.builder().addNamed("$internal:T.bytesDefaultValue(\"$default:L\")", m).build())
                        .build());
            }
        }

        @Override
        public void generateClearCode(MethodSpec.Builder method) {
            if (info.getDefaultValue().isEmpty()) {
                method.addNamedCode("$field:N.clear();\n", m);
            } else {
                method.addNamedCode("$field:N.setBytes($defaultField:N);\n", m);
            }
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            // setField(byte[])
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(BYTE_ARRAY, "buffer", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("return $setMethod:N(buffer, 0, buffer.length);\n", m)
                    .build());

            // setField(byte[], int, int)
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(BYTE_ARRAY, "buffer", Modifier.FINAL)
                    .addParameter(int.class, "offset", Modifier.FINAL)
                    .addParameter(int.class, "length", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.setBytes(buffer, offset, length);\n" +
                            "return this;\n", m)
                    .build());

            // setFieldLength(int)
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName() + "Length")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "length", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.setLength(length);\n" +
                            "return this;\n", m)
                    .build());
        }

        @Override
        protected void generateGetter(TypeSpec.Builder type) {
            // int getFieldLength()
            type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Length")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(int.class)
                    .addNamedCode("return $field:N.length();\n", m)
                    .build());

            // byte[] getFieldArray()
            CodeBlock link = CodeBlock.of("{@link $1N $1N}", info.getGetterName() + "Length");
            type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Array")
                    .addJavadoc("" +
                            "Only valid up to " + link + " bytes.\n" +
                            "\n" +
                            "@return underlying array\n")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(BYTE_ARRAY)
                    .addNamedCode("return $field:N.array();\n", m)
                    .build());

        }

    }

    static class RepeatedBytesField extends RepeatedField {

        protected RepeatedBytesField(RequestInfo.FieldInfo info) {
            super(info);
            m.put("setMethod", info.getSetterName());
            m.put("addMethod", info.getAdderName());
        }

        @Override
        public void generateMergingCode(MethodSpec.Builder method) {
            method
                    .addNamedCode("final int arrayLength = $wireFormat:T.getRepeatedFieldArrayLength(input, $tag:L);\n", m)
                    .addNamedCode("$field:N.ensureSpace(arrayLength);\n", m)
                    .beginControlFlow("for (int i = 0; i < arrayLength - 1; i++)")
                    .addNamedCode("input.readBytes($field:N.getAndAdd());\n", m)
                    .addStatement("input.readTag()")
                    .endControlFlow()
                    .addNamedCode("input.readBytes($field:N.getAndAdd());\n", m)
                    .addNamedCode("$setHas:L;\n", m);
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            // setField(byte[])
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .addParameter(BYTE_ARRAY, "buffer", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("return $setMethod:N(index, buffer, 0, buffer.length);\n", m)
                    .build());

            // setField(byte[], int, int)
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .addParameter(BYTE_ARRAY, "buffer", Modifier.FINAL)
                    .addParameter(int.class, "offset", Modifier.FINAL)
                    .addParameter(int.class, "length", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.get(index).setBytes(buffer, offset, length);\n" +
                            "return this;\n", m)
                    .build());

            // setFieldLength(int)
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName() + "Length")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .addParameter(int.class, "length", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.get(index).setLength(length);\n" +
                            "return this;\n", m)
                    .build());

            // addField(byte[])
            type.addMethod(MethodSpec.methodBuilder(info.getAdderName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(BYTE_ARRAY, "buffer", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("return $addMethod:N(buffer, 0, buffer.length);\n", m)
                    .build());

            // addField(byte[], int, int)
            type.addMethod(MethodSpec.methodBuilder(info.getAdderName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(BYTE_ARRAY, "buffer", Modifier.FINAL)
                    .addParameter(int.class, "offset", Modifier.FINAL)
                    .addParameter(int.class, "length", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$field:N.add(buffer, offset, length);\n" +
                            "return this;\n", m)
                    .build());
        }

        @Override
        protected void generateGetter(TypeSpec.Builder type) {
            generateGetCount(type);

            // int getFieldLength(int)
            type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Length")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .returns(int.class)
                    .addNamedCode("return $field:N.get(index).length();\n", m)
                    .build());

            // byte[] getFieldArray(int)
            CodeBlock link = CodeBlock.of("{@link $1N(int) $1N(index)}", info.getGetterName() + "Length");
            type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Array")
                    .addJavadoc("" +
                            "Only valid up to " + link + " bytes.\n" +
                            "\n" +
                            "@return underlying array\n")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .returns(BYTE_ARRAY)
                    .addNamedCode("return $field:N.get(index).array();\n", m)
                    .build());

        }

    }

}
