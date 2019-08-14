package us.hebi.robobuf.compiler.field;

import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class MessageField {

    static class OptionalMessageField extends FieldGenerator {

        OptionalMessageField(RequestInfo.FieldInfo info) {
            super(info);
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {
            MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(info.getParentType())
                    .addParameter(fieldType, "value")
                    .addNamedCode("$field:N.copyFrom(value);\n", m)
                    .addNamedCode("$setHas:L;\n", m)
                    .addStatement("return this")
                    .build();
            type.addMethod(setter);
        }

        private boolean isGroup() {
            return info.getDescriptor().getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP;
        }

    }

    static class RepeatedMessageField extends RepeatedReferenceField {

        protected RepeatedMessageField(RequestInfo.FieldInfo info) {
            super(info);
        }

    }

}
