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
    public void generateMembers(TypeSpec.Builder type) {

        FieldSpec value = FieldSpec.builder(typeName, info.getLowerName())
                .addModifiers(Modifier.PRIVATE)
                .build();

        type.addField(value);

    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("this.$name:L = $default:L;\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("this.$name:L = other.$name:L;\n", m);
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

    private String getDefaultValue() {
        String value = info.getDescriptor().getDefaultValue();
        if (value.isEmpty()) {
            switch (info.getDescriptor().getType()) {
                case TYPE_DOUBLE:
                    return "0d";
                case TYPE_FLOAT:
                    return "0f";
                case TYPE_BOOL:
                    return "false";
                default:
                    return "0";
            }
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
            return value + 'f';
        if (typeName == TypeName.DOUBLE && lastChar != 'D')
            return value + 'd';
        if (typeName == TypeName.LONG && lastChar != 'L')
            return value + 'L'; // lower case L may make it difficult to read number 1 (l=1?)
        return value;
    }

}
