package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
class StringField {

    static class OptionalStringField extends FieldGenerator {

        protected OptionalStringField(RequestInfo.FieldInfo info) {
            super(info);
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(CharSequence.class, "value")
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.setLength(0);\n" +
                            "$field:N.append(value);\n" +
                            "return this;\n", m)
                    .build();
            type.addMethod(setter);
        }

    }

    static class RepeatedStringField extends RepeatedReferenceField {

        protected RepeatedStringField(RequestInfo.FieldInfo info) {
            super(info);
        }

    }

}
