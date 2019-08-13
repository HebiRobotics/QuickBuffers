package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import us.hebi.robobuf.compiler.RequestInfo;

/**
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public abstract class RepeatedReferenceField extends RepeatedField {

    protected RepeatedReferenceField(RequestInfo.FieldInfo info) {
        super(info);
        m.put("secondArgs", info.isGroup() ? ", " + info.getNumber() : "");
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        method
                .addNamedCode("final int arrayLength = $wireFormat:T.getRepeatedFieldArrayLength(input, $tag:L);\n", m)
                .addNamedCode("$field:N.ensureSpace(arrayLength);\n", m)
                .beginControlFlow("for (int i = 0; i < arrayLength - 1; i++)")
                .addNamedCode("input.read$capitalizedType:L($field:N.getAndAdd()$secondArgs:L);\n", m)
                .addStatement("input.readTag()")
                .endControlFlow()
                .addNamedCode("input.read$capitalizedType:L($field:N.getAndAdd()$secondArgs:L);\n", m)
                .addNamedCode("$setHas:L;\n", m);
    }

}
