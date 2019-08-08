package us.hebi.robobuf.compiler.field;

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
    public void generateMembers(TypeSpec.Builder type) {

        FieldSpec value = FieldSpec.builder(typeName, info.getLowerName())
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", typeName)
                .build();

        MethodSpec getter = MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(value.type)
                .addStatement("return $L", value.name)
                .build();

        MethodSpec mutableGetter = MethodSpec.methodBuilder(info.getMutableGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(value.type)
                .addStatement(info.getSetBit())
                .addStatement("return $L", value.name)
                .build();

        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addParameter(value.type, "value")
                .addStatement("$L.copyFrom(value)", value.name)
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

        type.addField(value);
        type.addMethod(getter);
        type.addMethod(mutableGetter);
        type.addMethod(setter);
        type.addMethod(hazzer);
        type.addMethod(clearer);

    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("this.$name:L.clear();\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addStatement("this.$1L.copyFrom(other.$1L)", info.getLowerName());
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {

    }

    @Override
    public void generateMergingCodeFromPacked(MethodSpec.Builder method) {

    }

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {

    }

    @Override
    public void generateSerializedSizeCode(MethodSpec.Builder method) {

    }

    @Override
    public void generateEqualsCode(MethodSpec.Builder method) {

    }

    @Override
    public void generateHashCodeCode(MethodSpec.Builder method) {

    }

}
