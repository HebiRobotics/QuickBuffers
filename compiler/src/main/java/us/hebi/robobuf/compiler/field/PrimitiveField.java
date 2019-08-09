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

    protected PrimitiveField(FieldInfo info) {
        super(info);
        m.put("default", getDefaultValue());
    }

    @Override
    public void generateField(TypeSpec.Builder type) {
        FieldSpec value = FieldSpec.builder(typeName, info.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        type.addField(value);
    }

    @Override
    public void generateMembers(TypeSpec.Builder type) {

        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value")
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$name:L = value;\n" +
                        "return this;\n", m)
                .build();

        MethodSpec clearer = MethodSpec.methodBuilder(info.getClearName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$clearHas:L;\n" +
                        "$name:L = $default:L;\n" +
                        "return this;\n", m)
                .build();

        generateHasAndGet(type);
        type.addMethod(setter);
        type.addMethod(clearer);

    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L = $default:L;\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L = other.$name:L;\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "$name:L = input.read$capitalizedType:L();\n" +
                "$setHas:L;\n", m);
    }

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "if ($getHas:L) {$>\n" +
                "output.write$capitalizedType:L($number:L, $name:L);\n" +
                "$<}\n", m);
    }

    @Override
    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "if ($getHas:L) {$>\n" +
                "size += $computeClass:T.compute$capitalizedType:LSize($number:L, $name:L);\n" +
                "$<}\n", m);
    }

    @Override
    public void generateEqualsCode(MethodSpec.Builder method) {
        method.addNamedCode("if ($getHas:L && ", m);
        if (typeName == TypeName.FLOAT)
            method.addNamedCode("(Float.floatToIntBits($name:L) != Float.floatToIntBits(other.$name:L))", m);
        else if (typeName == TypeName.DOUBLE)
            method.addNamedCode("(Double.doubleToLongBits($name:L) != Double.doubleToLongBits(other.$name:L))", m);
        else
            method.addNamedCode("($name:L != other.$name:L)", m);
        method.addNamedCode(") {$>\nreturn false;$<\n}\n", m);
    }

    private String getDefaultValue() {
        String value = info.getDescriptor().getDefaultValue();
        if (value.isEmpty()) {
            if (typeName == TypeName.FLOAT)
                return "0F";
            if (typeName == TypeName.DOUBLE)
                return "0D";
            if (typeName == TypeName.LONG)
                return "0L";
            if (typeName == TypeName.BOOLEAN)
                return "false";
            return "0";
        }

        // Special cased default values
        boolean isFloat = (typeName == TypeName.FLOAT);
        String constantClass = isFloat ? "Float" : "Double";
        switch (value) {
            case "nan":
                return constantClass + ".NaN";
            case "-inf":
                return constantClass + ".NEGATIVE_INFINITY";
            case "+inf":
            case "inf":
                return constantClass + ".POSITIVE_INFINITY";
        }

        // Add modifiers if needed
        char lastChar = Character.toUpperCase(value.charAt(value.length() - 1));

        if (typeName == TypeName.FLOAT && lastChar != 'F')
            return value + 'F';
        if (typeName == TypeName.DOUBLE && lastChar != 'D')
            return value + 'D';
        if (typeName == TypeName.LONG && lastChar != 'L')
            return value + 'L'; // lower case L may make it difficult to read number 1 (l=1?)
        return value;
    }

}
