package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
class EnumField extends PrimitiveField {

    EnumField(RequestInfo.FieldInfo info) {
        super(info);
        m.put("serializableValue", info.getFieldName() + ".getNumber()");
    }

    @Override
    protected String getDefaultValue() {
        String defaultValue = info.getDescriptor().getDefaultValue();
        return !defaultValue.isEmpty() ? info.getTypeName() + "." + defaultValue : "null";
    }

    protected void generateSetter(TypeSpec.Builder type) {
        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value")
                .returns(info.getParentType())
                .beginControlFlow("if (value == null)")
                .addNamedCode("$clearHas:L;\n", m)
                .nextControlFlow("else")
                .addNamedCode("$setHas:L;\n", m)
                .endControlFlow()
                .addNamedCode("$name:L = value;\n", m)
                .addStatement("return this")
                .build();

        type.addMethod(setter);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        method.addNamedCode("$type:T value = $type:T.forNumber(input.readInt32());\n", m)
                .beginControlFlow("if (value != null)")
                .addNamedCode("$name:N = value;\n", m)
                .addNamedCode("$setHas:L;\n", m)
                .endControlFlow();
    }

}
