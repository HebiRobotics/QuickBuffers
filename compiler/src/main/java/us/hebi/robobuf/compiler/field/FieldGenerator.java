package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;
import java.util.HashMap;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public abstract class FieldGenerator {

    public RequestInfo.FieldInfo getInfo() {
        return info;
    }

    public abstract void generateMemberFields(TypeSpec.Builder type);

    public void generateClearCode(MethodSpec.Builder method) {
        if (info.isNonRepeatedPrimitiveOrEnum()) {
            method.addNamedCode("$field:N = $default:L;\n", m);
        } else {
            method.addNamedCode("$field:N.clear();\n", m);
        }
    }

    public void generateCopyFromCode(MethodSpec.Builder method) {
        if (info.isNonRepeatedPrimitiveOrEnum()) {
            method.addNamedCode("$field:N = other.$field:N;\n", m);
        } else {
            method.addNamedCode("$field:N.copyFrom(other.$field:N);\n", m);
        }
    }

    public void generateMergingCode(MethodSpec.Builder method) {
        if (info.isPrimitive()) {
            method.addNamedCode("$field:N = input.read$capitalizedType:L();\n", m);
        }
        // What else? Messages etc.?
        method.addNamedCode("$setHas:L;\n", m);
    }

    public void generateMergingCodeFromPacked(MethodSpec.Builder method) {
//        throw new GeneratorException("Not a packable field: " + info.getDescriptor()); // only for repeated fields
    }

    public void generateSerializationCode(MethodSpec.Builder method) {
        method.addNamedCode("output.write$capitalizedType:L($number:L, $serializableValue:L);\n", m); // non-repeated
    }

    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        method.addNamedCode("size += $computeClass:T.compute$capitalizedType:LSize($number:L, $serializableValue:L);\n", m); // non-repeated
    }

    public void generateEqualsStatement(MethodSpec.Builder method) {
        method.addNamedCode("$field:N.equals(other.$field:N)", m); // non-primitive
    }

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
                .addNamedCode("return $field:N;\n", m)
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

    protected FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        typeName = info.getTypeName();

        // Common-variable map for named arguments
        m.put("field", info.getFieldName());
        m.put("default", info.getDefaultValue());
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
        m.put("wireFormat", RuntimeClasses.WIRE_FORMAT);
        m.put("internal", RuntimeClasses.INTERNAL);

    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;

    protected final HashMap<String, Object> m = new HashMap<>();

}
