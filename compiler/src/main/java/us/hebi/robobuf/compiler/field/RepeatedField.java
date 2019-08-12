package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 12 Aug 2019
 */
public class RepeatedField extends FieldGenerator {

    protected RepeatedField(RequestInfo.FieldInfo info) {
        super(info);
        m.put("repeatedType", info.getRepeatedStoreType());
        m.put("bytesPerTag", info.getBytesPerTag());
        m.put("packedTag", info.getPackedTag());
        if (info.isFixedWidth())
            m.put("fixedWidth", info.getFixedWidth());
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(info.getRepeatedStoreType(), info.getFieldName(), Modifier.FINAL, Modifier.PRIVATE)
                .initializer("new $T()", info.getRepeatedStoreType())
                .build());
    }

 /*   @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        if (info.isPrimitive()) {
            // non-packed fields aren't expected to be used much, so do something dumb for now
            method.addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m);
        }
        // What else?
        method.addNamedCode("$setHas:L;\n", m);
    }*/

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {
        // Non-packed
        method.addNamedCode("" +
                "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                "output.write$capitalizedType:L($number:L, $field:N.get(i));\n" +
                "$<}\n", m);
    }

    @Override
    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        // Non-packed
        method.addNamedCode("" +
                "int dataSize = 0;\n" +
                "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                "dataSize += $computeClass:T.compute$capitalizedType:LSizeNoTag($field:N.get(i));\n" +
                "$<}\n" +
                "size += dataSize;\n" +
                "size += $bytesPerTag:L * $field:N.length();\n", m);
    }

    @Override
    protected void generateGetter(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Count")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addNamedCode("return $field:N.length();\n", m)
                .build());

        /*if (info.isPrimitive()) {



        } else if (info.isEnum()) {

            type.addMethod(MethodSpec.methodBuilder(info.getGetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .returns(typeName)
                    .addNamedCode("return $type:T.forNumber($field:N.get(index));\n", m)
                    .build());

        } else {

        }*/
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {

    }

}
