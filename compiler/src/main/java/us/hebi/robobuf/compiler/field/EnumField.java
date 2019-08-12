package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
class EnumField {

    static class OptionalEnumField extends FieldGenerator {

        OptionalEnumField(RequestInfo.FieldInfo info) {
            super(info);
            String defaultValue = info.getDescriptor().getDefaultValue();
            m.put("default", !defaultValue.isEmpty() ? info.getTypeName() + "." + defaultValue + ".getNumber()" : "0");
        }

        @Override
        public void generateMemberFields(TypeSpec.Builder type) {
            FieldSpec value = FieldSpec.builder(int.class, info.getFieldName())
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            type.addField(value);
        }

        @Override
        protected void generateGetter(TypeSpec.Builder type) {

        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value")
                    .returns(info.getParentType())
                    .beginControlFlow("if (value == null)")
                    .addStatement("throw new $T()", NullPointerException.class)
                    .endControlFlow()
                    .addNamedCode("$setHas:L;\n", m)
                    .addNamedCode("$field:N = value.getNumber();\n", m)
                    .addStatement("return this")
                    .build();

            type.addMethod(setter);
        }

        @Override
        public void generateMergingCode(MethodSpec.Builder method) {
            method.addStatement("final int value = input.readInt32()")
                    .beginControlFlow("if ( $T.forNumber(value) != null )", typeName)
                    .addNamedCode("$field:N = value;\n", m)
                    .addNamedCode("$setHas:L;\n", m)
                    .endControlFlow();
        }

        @Override
        public void generateEqualsStatement(MethodSpec.Builder method) {
            method.addNamedCode("($field:N == other.$field:N)", m);
        }

    }
}
