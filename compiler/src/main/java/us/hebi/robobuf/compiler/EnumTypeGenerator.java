package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.squareup.javapoet.*;
import us.hebi.robobuf.compiler.RequestInfo.EnumInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class EnumTypeGenerator {

    EnumTypeGenerator(EnumInfo info) {
        this.info = info;
    }

    TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.enumBuilder(info.getTypeName())
                .addModifiers(Modifier.PUBLIC);

        for (EnumValueDescriptorProto value : info.getValues()) {
            type.addEnumConstant(NameUtil.filterKeyword(value.getName()), TypeSpec.anonymousClassBuilder("$L", value.getNumber()).build());
        }

        generateGetValue(type);
        generateForNumber(type);
        generateConstructor(type);
        return type.build();
    }

    private void generateConstructor(TypeSpec.Builder typeSpec) {
        typeSpec.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(int.class, "value")
                .addStatement("this.$1N = $1N", "value")
                .build());
        typeSpec.addField(FieldSpec.builder(int.class, "value", Modifier.PRIVATE, Modifier.FINAL).build());
    }

    private void generateGetValue(TypeSpec.Builder typeSpec) {
        typeSpec.addMethod(MethodSpec.methodBuilder("getNumber")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return value")
                .build());
    }

    private void generateForNumber(TypeSpec.Builder typeSpec) {

        MethodSpec.Builder forNumber = MethodSpec.methodBuilder("forNumber")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(info.getTypeName())
                .addParameter(TypeName.INT, "value");

        if (info.isUsingArrayLookup()) {

            // (fast) lookup using array index
            forNumber.beginControlFlow("if (value < 0 || value > lookup.length)", info.getHighestNumber())
                    .addStatement("return null")
                    .endControlFlow();
            forNumber.addStatement("return lookup[value]");

            TypeName arrayType = ArrayTypeName.of(info.getTypeName());
            typeSpec.addField(FieldSpec.builder(arrayType, "lookup", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T[$L]", info.getTypeName(), info.getHighestNumber() + 1)
                    .build());

            CodeBlock.Builder initBlock = CodeBlock.builder();
            for (EnumValueDescriptorProto value : info.getValues()) {
                initBlock.addStatement("lookup[$L] = $L", value.getNumber(), NameUtil.filterKeyword(value.getName()));
            }
            typeSpec.addStaticBlock(initBlock.build());

        } else {

            // lookup using switch statement
            forNumber.beginControlFlow("switch(value)");
            for (EnumValueDescriptorProto value : info.getValues()) {
                forNumber.addStatement("case $L: return $L", value.getNumber(), NameUtil.filterKeyword(value.getName()));
            }
            forNumber.addStatement("default: return null");
            forNumber.endControlFlow();

        }

        typeSpec.addMethod(forNumber.build());

    }

    final EnumInfo info;

}
