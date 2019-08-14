package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

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
        }

        @Override
        protected void generateSetMethods(TypeSpec.Builder type) {
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
                            "$field:N.copyFrom(buffer, offset, length);\n" +
                            "return this;\n", m)
                    .build());

        }

    }

    static class RepeatedBytesField extends FieldGenerator {

        protected RepeatedBytesField(RequestInfo.FieldInfo info) {
            super(info);
        }

        @Override
        protected void generateSetMethods(TypeSpec.Builder type) {
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

    }

}
