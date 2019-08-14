package us.hebi.robobuf.compiler.field;

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
        }

        @Override
        protected void generateGetMethods(TypeSpec.Builder type) {
            MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName);
            if (!info.hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N);\n", m);
            } else {
                getter.addNamedCode("" +
                        "final $type:T result = $type:T.forNumber($field:N);\n" +
                        "return result == null ? $defaultEnumValue:L : result;\n", m);
            }
            type.addMethod(getter.build());
        }

    }

    static class RepeatedEnumField extends FieldGenerator {

        protected RepeatedEnumField(RequestInfo.FieldInfo info) {
            super(info);
        }

        @Override
        protected void generateGetMethods(TypeSpec.Builder type) {
            MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .returns(typeName);
            if (!info.hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N.get(index));\n", m);
            } else {
                getter.addNamedCode("" +
                        "final $type:T result = $type:T.forNumber($field:N.get(index));\n" +
                        "return result == null ? $defaultEnumValue:L : result;\n", m);
            }
        }

    }

}
