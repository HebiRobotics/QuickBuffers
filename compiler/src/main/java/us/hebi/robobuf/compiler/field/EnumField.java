package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
public class EnumField extends FieldGenerator {

    protected EnumField(RequestInfo.FieldInfo info) {
        super(info);
        String defaultValue = info.getDescriptor().getDefaultValue();
        m.put("default", !defaultValue.isEmpty() ? info.getTypeName() + "." + defaultValue : "null");
    }

    // TODO: protobuf-java stores int and converts to enum lazily. Maybe do that as well?
    @Override
    public void generateMembers(TypeSpec.Builder type) {

        FieldSpec value = FieldSpec.builder(typeName, info.getLowerName())
                .addModifiers(Modifier.PRIVATE)
                .build();

        MethodSpec hazzer = MethodSpec.methodBuilder(info.getHazzerName())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addNamedCode("return $getHas:L;\n", m)
                .build();

        MethodSpec getter = MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(value.type)
                .addStatement("return $L", info.getLowerName())
                .build();

        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value")
                .returns(info.getParentType())
                .beginControlFlow("if(value == null)")
                .addNamedCode("$clearHas:L;\n", m)
                .nextControlFlow("else")
                .addNamedCode("$setHas:L;\n", m)
                .endControlFlow()
                .addNamedCode("$name:L = value;\n", m)
                .addStatement("return this")
                .build();

        MethodSpec clearer = MethodSpec.methodBuilder(info.getClearName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$clearHas:L;\n" +
                        "$name:L = $default:L;\n" +
                        "return this;\n", m)
                .build();

        type.addMethod(hazzer);
        type.addMethod(getter);
        type.addMethod(setter);
        type.addMethod(clearer);
        type.addField(value);

    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L = $default:L;\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:N = other.$name:N;\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        method.addNamedCode("$type:T value = $type:T.forNumber(input.readInt32());\n", m)
                .beginControlFlow("if (value != null)")
                .addNamedCode("$name:N = value;\n", m)
                .addNamedCode("$setHas:L;\n", m)
                .endControlFlow();
    }

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "if ($getHas:L) {$>\n" +
                "output.writeInt32($number:L, $name:N.getNumber());\n" +
                "$<}\n", m);
    }

    @Override
    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "if ($getHas:L) {$>;\n" +
                "size += $computeClass:T.computeInt32Size($number:L, $name:N.getNumber());\n" +
                "$<}\n", m);
    }

    @Override
    public void generateEqualsCode(MethodSpec.Builder method) {
        method.addNamedCode("if ($getHas:L && $name:N != other.$name:N)) {$>", m)
                .addStatement("return false")
                .endControlFlow();
    }

}
