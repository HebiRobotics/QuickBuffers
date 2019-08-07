package us.hebi.robobuf.compiler.type;

import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class EnumGenerator implements TypeGenerator {

    public EnumGenerator(EnumDescriptorProto descriptor) {
        this.descriptor = descriptor;
        this.typeName = ClassName.bestGuess(descriptor.getName());
    }

    public TypeSpec generate(boolean nested) {
        ClassName thisType = ClassName.bestGuess(descriptor.getName());
        TypeSpec.Builder type = TypeSpec.enumBuilder(thisType)
                .addModifiers(Modifier.PUBLIC);

        for (EnumValueDescriptorProto value : descriptor.getValueList()) {
            type.addEnumConstant(value.getName(), TypeSpec.anonymousClassBuilder("$L", value.getNumber()).build());
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

        int highestNumber = descriptor.getValueList().stream()
                .mapToInt(EnumValueDescriptorProto::getNumber)
                .max().orElseGet(() -> 0);

        MethodSpec.Builder forNumber = MethodSpec.methodBuilder("forNumber")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(typeName)
                .addParameter(TypeName.INT, "value");

        if (highestNumber > LOOKUP_THRESHOLD) {

            // lookup using switch statement
            forNumber.beginControlFlow("switch(value)");
            for (EnumValueDescriptorProto value : descriptor.getValueList()) {
                forNumber.addStatement("case $L: return $L", value.getNumber(), value.getName());
            }
            forNumber.addStatement("default: return null");
            forNumber.endControlFlow();

        } else {

            // lookup using array index
            forNumber.beginControlFlow("if(value < 0 || value > lookup.length)", highestNumber)
                    .addStatement("return null")
                    .endControlFlow();
            forNumber.addStatement("return lookup[value]");

            TypeName arrayType = ArrayTypeName.of(typeName);
            typeSpec.addField(FieldSpec.builder(arrayType, "lookup", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T[$L]", typeName, highestNumber + 1)
                    .build());

            CodeBlock.Builder initBlock = CodeBlock.builder();
            for (EnumValueDescriptorProto value : descriptor.getValueList()) {
                initBlock.addStatement("lookup[$L] = $L", value.getNumber(), value.getName());
            }
            typeSpec.addStaticBlock(initBlock.build());

        }

        typeSpec.addMethod(forNumber.build());

    }

    final EnumDescriptorProto descriptor;
    final ClassName typeName;
    private static final int LOOKUP_THRESHOLD = 50;

}
