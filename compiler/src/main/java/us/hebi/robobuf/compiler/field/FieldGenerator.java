package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.GeneratorException;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;
import java.util.HashMap;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public abstract class FieldGenerator {

    public abstract void generateMemberFields(TypeSpec.Builder type);

    public abstract void generateClearCode(MethodSpec.Builder method);

    public abstract void generateCopyFromCode(MethodSpec.Builder method);

    public abstract void generateMergingCode(MethodSpec.Builder method);

    public void generateMergingCodeFromPacked(MethodSpec.Builder method) {
        throw new GeneratorException("Merging from packed not implemented"); // only for repeated fields
    }

    public void generateSerializationCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "if ($getHas:L) {$>\n" +
                "output.write$capitalizedType:L($number:L, $serializableValue:L);\n" +
                "$<}\n", m);
    }

    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "if ($getHas:L) {$>\n" +
                "size += $computeClass:T.compute$capitalizedType:LSize($number:L, $serializableValue:L);\n" +
                "$<}\n", m);
    }

    public void generateEqualsCode(MethodSpec.Builder method) {
        method.addNamedCode("if ($hasMethod:N() && " + getNamedNotEqualsStatement() + ") {$>\n", m)
                .addStatement("return false")
                .endControlFlow();
    }

    protected abstract String getNamedNotEqualsStatement();

    protected abstract void generateSetter(TypeSpec.Builder type);

    public void generateMemberMethods(TypeSpec.Builder type) {
        generateHazzer(type);
        generateGetter(type);
        generateSetter(type);
        generateClearer(type);
    }

    protected void generateHazzer(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getHazzerName())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addNamedCode("return $getHas:L;\n", m)
                .build());
    }

    protected void generateGetter(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder(info.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addNamedCode("return $name:N;\n", m)
                .build());
    }

    protected void generateClearer(TypeSpec.Builder type) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(info.getClearName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .addNamedCode("$clearHas:L;\n", m);
        generateClearCode(method);
        method.addStatement("return this");
        type.addMethod(method.build());
    }

    public int getTag() {
        return info.getTag();
    }

    protected FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        typeName = info.getTypeName();

        // Common-variable map for named arguments
        m.put("name", info.getFieldName());
        m.put("hasMethod", info.getHazzerName());
        m.put("getHas", info.getHasBit());
        m.put("setHas", info.getSetBit());
        m.put("clearHas", info.getClearBit());
        m.put("message", info.getParentType());
        m.put("type", typeName);
        m.put("number", info.getNumber());
        m.put("tag", info.getTag());
        m.put("capitalizedType", RuntimeClasses.getCapitalizedType(info.getDescriptor().getType()));
        m.put("serializableValue", info.getFieldName());
        m.put("computeClass", RuntimeClasses.PROTO_DEST);
        m.put("roboUtil", RuntimeClasses.ROBO_UTIL);

    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;
    protected HashMap<String, Object> m = new HashMap<>();

}
