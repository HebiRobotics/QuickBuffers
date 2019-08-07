package us.hebi.robobuf.compiler.field;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.TypeMap;

import javax.lang.model.element.Modifier;

import static us.hebi.robobuf.compiler.field.FieldUtil.*;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class MessageField implements FieldGenerator {

    public MessageField(FieldDescriptorProto descriptor, TypeMap typeMap, int fieldIndex) {
        this.descriptor = descriptor;
        this.typeMap = typeMap;
        this.typeName = typeMap.getClassName(descriptor.getTypeName());
        this.fieldIndex = fieldIndex;
        this.fieldName = descriptor.getName();
    }

    @Override
    public void generateMembers(TypeSpec.Builder type) {

        FieldSpec value = FieldSpec.builder(typeName, memberName(fieldName))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", typeName)
                .build();

        MethodSpec getter = MethodSpec.methodBuilder(getterName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(value.type)
                .addStatement("return $L", value.name)
                .build();

        MethodSpec mutableGetter = MethodSpec.methodBuilder(mutableGetterName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(value.type)
                .addStatement(BitField.setBit(fieldIndex))
                .addStatement("return $L", value.name)
                .build();

        MethodSpec setter = MethodSpec.methodBuilder(setterName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class) // TODO: parent type
                .addParameter(value.type, "value")
                .addStatement("$L.copyFrom(value)", value.name)
                .addStatement(BitField.setBit(fieldIndex))
//                .addStatement("return this")
                .build();

        MethodSpec hazzer = MethodSpec.methodBuilder(hazzerName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L", BitField.hasBit(fieldIndex))
                .build();

        MethodSpec clearer = MethodSpec.methodBuilder(clearerName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class) // TODO: parent type
                .addStatement(BitField.clearBit(fieldIndex))
                .addStatement("$L.clear()", value.name)
//                .addStatement("return this")
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

    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addStatement("this.$1L.copyFrom(other.$1L)", memberName(fieldName));
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

    private final FieldDescriptorProto descriptor;
    private final TypeMap typeMap;
    private final TypeName typeName;
    private final int fieldIndex;
    private final String fieldName;

}
