package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo.FieldInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
class PrimitiveField extends FieldGenerator {

    PrimitiveField(FieldInfo info) {
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
                .addParameter(info.getTypeName(), "value")
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
            method.addNamedCode("(Float.floatToIntBits($field:N) == Float.floatToIntBits(other.$field:N))", m);
        } else if (typeName == TypeName.DOUBLE) {
            method.addNamedCode("(Double.doubleToLongBits($field:N) == Double.doubleToLongBits(other.$field:N))", m);
        } else {
            method.addNamedCode("($field:N == other.$field:N)", m);
        }
    }

}
