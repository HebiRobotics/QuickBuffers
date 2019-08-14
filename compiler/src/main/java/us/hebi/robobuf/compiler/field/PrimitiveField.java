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
class PrimitiveField {

    static class OptionalPrimitiveField extends FieldGenerator {

        OptionalPrimitiveField(RequestInfo.FieldInfo info) {
            super(info);
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

    }

    static class RepeatedPrimitiveField extends RepeatedField {

        RepeatedPrimitiveField(RequestInfo.FieldInfo info) {
            super(info);
        }

    }
}
