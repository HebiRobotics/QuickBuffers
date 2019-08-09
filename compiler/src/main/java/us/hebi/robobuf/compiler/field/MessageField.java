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
        m.put("groupOrMessage", isGroup() ? "Group" : "Message");
    }

    @Override
    public void generateField(TypeSpec.Builder type) {
        FieldSpec value = FieldSpec.builder(typeName, info.getFieldName())
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", typeName)
                .build();
        type.addField(value);
    }

    @Override
    public void generateMembers(TypeSpec.Builder type) {

        MethodSpec getter = MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement("return $L", info.getFieldName())
                .build();

        MethodSpec mutableGetter = MethodSpec.methodBuilder(info.getMutableGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement(info.getSetBit())
                .addStatement("return $L", info.getFieldName())
                .build();

        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addParameter(typeName, "value")
                .addStatement("$L.copyFrom(value)", info.getFieldName())
                .addStatement(info.getSetBit())
                .addStatement("return this")
                .build();

        MethodSpec hazzer = MethodSpec.methodBuilder(info.getHazzerName())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L", info.getHasBit())
                .build();

        MethodSpec clearer = MethodSpec.methodBuilder(info.getClearName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$clearHas:L;\n" +
                        "$name:L.clear();\n" +
                        "return this;\n", m)
                .build();

        type.addMethod(hazzer);
        type.addMethod(getter);
        type.addMethod(mutableGetter);
        type.addMethod(setter);
        type.addMethod(clearer);

    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L.clear();\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L.copyFrom(other.$name:L);\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        if (isGroup()) {
            method.addNamedCode("input.readGroup(this.$name:L, $number:L);\n", m);
        } else {
            method.addNamedCode("input.readMessage(this.$name:L);\n", m);
        }
        method.addNamedCode("$setHas:L;\n", m);
    }

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {
        method.addNamedCode("if ($getHas:L) {$>\n", m);
        method.addNamedCode("output.write$groupOrMessage:L($number:L, this.$name:L);\n", m);
        method.endControlFlow();
    }

    @Override
    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        method.addNamedCode("if ($getHas:L) {$>\n", m);
        method.addNamedCode("size += $computeClass:T.compute$groupOrMessage:LSize($number:L, this.$name:L);\n", m);
        method.endControlFlow();
    }

    @Override
    public void generateEqualsCode(MethodSpec.Builder method) {
        method.addNamedCode("if ($getHas:L && !$name:L.equals(other.$name:L)) {$>\n", m);
        method.addStatement("return false");
        method.endControlFlow();
    }

    private boolean isGroup() {
        return info.getDescriptor().getType() == Type.TYPE_GROUP;
    }

}
