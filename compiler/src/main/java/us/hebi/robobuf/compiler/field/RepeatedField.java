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
public abstract class RepeatedField extends FieldGenerator {

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
                .addJavadoc(info.getJavadoc())
                .initializer("new $T()", info.getRepeatedStoreType())
                .build());
    }

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
        generateGetCount(type);
        generateGetIndex(type);
    }

    protected void generateGetCount(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName() + "Count")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Deprecated.class) // TODO: replace with getField() / getMutableField()
                .returns(int.class)
                .addNamedCode("return $field:N.length();\n", m)
                .build());
    }

    protected void generateGetIndex(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Deprecated.class)
                .addParameter(int.class, "index", Modifier.FINAL)
                .returns(typeName)
                .addNamedCode("return $field:N.get(index);\n", m)
                .build());
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "index", Modifier.FINAL)
                .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$field:N.set(index, value);\n" +
                        "return this;\n", m)
                .build());

        type.addMethod(MethodSpec.methodBuilder("add" + info.getUpperName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$field:N.add(value);\n" +
                        "return this;\n", m)
                .build());
    }

}
