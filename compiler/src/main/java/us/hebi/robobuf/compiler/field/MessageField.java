package us.hebi.robobuf.compiler.field;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class MessageField extends FieldGenerator {

    public MessageField(RequestInfo.FieldInfo info) {
        super(info);
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        FieldSpec value = FieldSpec.builder(typeName, info.getFieldName())
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", typeName)
                .build();
        type.addField(value);
    }

    @Override
    protected void generateGetter(TypeSpec.Builder type){
        super.generateGetter(type);
        MethodSpec mutableGetter = MethodSpec.methodBuilder(info.getMutableGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addNamedCode("$setHas:L;\n", m)
                .addNamedCode("return $field:N;\n", m)
                .build();
        type.addMethod(mutableGetter);
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {
        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addParameter(typeName, "value")
                .addNamedCode("$field:N.copyFrom(value);\n", m)
                .addNamedCode("$setHas:L;\n", m)
                .addStatement("return this")
                .build();
        type.addMethod(setter);
    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$field:N.clear();\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        if (isGroup()) {
            method.addNamedCode("input.readGroup($field:N, $number:L);\n", m);
        } else {
            method.addNamedCode("input.readMessage($field:N);\n", m);
        }
        method.addNamedCode("$setHas:L;\n", m);
    }

    private boolean isGroup() {
        return info.getDescriptor().getType() == Type.TYPE_GROUP;
    }

}
