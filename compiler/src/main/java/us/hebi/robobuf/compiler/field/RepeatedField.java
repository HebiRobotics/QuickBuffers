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
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(info.getRepeatedStoreType(), info.getFieldName(), Modifier.FINAL, Modifier.PRIVATE)
                .initializer("new $T()", info.getRepeatedStoreType())
                .build());
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        if (info.isPrimitive()) {
            // non-packed fields aren't expected to be used much, so do something dumb for now
            method.addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m);
        }
        // What else?
        method.addNamedCode("$setHas:L;\n", m);
    }

    @Override
    public void generateMergingCodeFromPacked(MethodSpec.Builder method) {
        method.addNamedCode("input.readPacked$capitalizedType:L($field:N);\n", m);
        method.addNamedCode("$setHas:L;\n", m);
    }

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {
        if (info.isPacked()) {
            method.addNamedCode("output.writePacked$capitalizedType:L($number:L, $field:N);\n", m);
        } else {
            method.addNamedCode("" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "output.write$capitalizedType:L($number:L, $field:N.get(i));\n" +
                    "$<}\n", m);
        }
    }

    @Override
    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        if (info.isPacked()) {
            method.addNamedCode("size += $computeClass:T.computePacked$capitalizedType:LSize($number:L, $field:N);\n", m);
        } else {
            method.addNamedCode("" +
                    "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                    "size += $computeClass:T.compute$capitalizedType:LSize($number:L, $field:N.get(i));\n" +
                    "$<}\n", m);
        }
    }

    @Override
    protected void generateGetter(TypeSpec.Builder type) {

    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {

    }

}
