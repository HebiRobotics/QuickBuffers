package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
class EnumField {

    static class OptionalEnumField extends FieldGenerator {

        OptionalEnumField(RequestInfo.FieldInfo info) {
            super(info);
            String defaultValue = info.getDefaultValue();
            m.put("default", hasDefaultValue() ? info.getTypeName() + "." + defaultValue + ".getNumber()" : "0");
            m.put("defaultReturnValue", info.getTypeName() + "." + defaultValue);
        }

        private boolean hasDefaultValue() {
            return !info.getDefaultValue().isEmpty();
        }

        @Override
        protected void generateGetMethods(TypeSpec.Builder type) {
            MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName);
            if (!hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N);\n", m);
            } else {
                getter.addNamedCode("" +
                        "final $type:T result = $type:T.forNumber($field:N);\n" +
                        "return result == null ? $defaultReturnValue:L : result;\n", m);
            }
            type.addMethod(getter.build());
        }

        @Override
        protected void generateSetMethods(TypeSpec.Builder type) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value")
                    .returns(info.getParentType())
                    .addNamedCode("$field:N = value.getNumber();\n", m)
                    .addNamedCode("$setHas:L;\n", m)
                    .addStatement("return this")
                    .build();

            type.addMethod(setter);
        }

    }

    static class RepeatedEnumField extends FieldGenerator {

        protected RepeatedEnumField(RequestInfo.FieldInfo info) {
            super(info);
            String defaultValue = info.getDefaultValue();
            m.put("default", hasDefaultValue() ? info.getTypeName() + "." + defaultValue + ".getNumber()" : "0");
            m.put("defaultReturnValue", info.getTypeName() + "." + defaultValue);
        }

        private boolean hasDefaultValue() {
            return !info.getDefaultValue().isEmpty();
        }

        @Override
        protected void generateGetMethods(TypeSpec.Builder type) {
            MethodSpec.Builder getter = MethodSpec.methodBuilder(info.getGetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .returns(typeName);
            if (!hasDefaultValue()) {
                getter.addNamedCode("return $type:T.forNumber($field:N.get(index));\n", m);
            } else {
                getter.addNamedCode("" +
                        "final $type:T result = $type:T.forNumber($field:N.get(index));\n" +
                        "return result == null ? $defaultReturnValue:L : result;\n", m);
            }
        }

        @Override
        protected void generateSetMethods(TypeSpec.Builder type) {
            type.addMethod(MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(int.class, "index", Modifier.FINAL)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.set(index, value.getNumber());\n" +
                            "return this;\n", m)
                    .build());

            type.addMethod(MethodSpec.methodBuilder("add" + info.getUpperName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(info.getTypeName(), "value", Modifier.FINAL)
                    .returns(info.getParentType())
                    .addNamedCode("" +
                            "$setHas:L;\n" +
                            "$field:N.add(value.getNumber());\n" +
                            "return this;\n", m)
                    .build());
        }

    }

}
