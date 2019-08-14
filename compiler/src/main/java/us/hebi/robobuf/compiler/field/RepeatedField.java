package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 12 Aug 2019
 */
public abstract class RepeatedField extends FieldGenerator {

    protected RepeatedField(RequestInfo.FieldInfo info) {
        super(info);
    }

    @Override
    protected void generateGetter(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getRepeatedStoreType())
                .addNamedCode("return $field:N;\n", m)
                .build());

        type.addMethod(MethodSpec.methodBuilder(info.getMutableGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getRepeatedStoreType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "return $field:N;\n", m)
                .build());
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder("add" + info.getUpperName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$field:N.add(value);\n" +
                        "return this;\n", m)
                .build());

        if (info.isPrimitive()) {
            type.addMethod(MethodSpec.methodBuilder("addAll" + info.getUpperName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ArrayTypeName.of(info.getTypeName()), "values", Modifier.FINAL)
                    .varargs(true)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.addAll(values);\n" +
                            "return this;\n", m)
                    .build());
        }
    }

}
