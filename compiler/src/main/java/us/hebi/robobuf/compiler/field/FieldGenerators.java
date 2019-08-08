package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo.FieldInfo;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerators {

    public static FieldGenerator createGenerator(FieldInfo field) {
        if (field.isPrimitive() && !field.isRepeated()) {
            return new PrimitiveField(field);
        }

        switch (field.getDescriptor().getType()) {
            case TYPE_MESSAGE:
                if (!field.isRepeated())
                    return new MessageField(field);
                break;

            case TYPE_ENUM:
                if (!field.isRepeated())
                    return new EnumField(field);
                break;

            case TYPE_STRING:
                break;
            case TYPE_BYTES:
                break;
            case TYPE_GROUP:
                break;
        }

        return new IgnoredFieldGenerator(field);
    }

    static final class IgnoredFieldGenerator extends FieldGenerator {

        protected IgnoredFieldGenerator(FieldInfo info) {
            super(info);
        }

        @Override
        public void generateMembers(TypeSpec.Builder type) {
            type.addField(FieldSpec.builder(info.getTypeName(), info.getLowerName()).addJavadoc("Unsupported Field Type").build());
        }

        @Override
        public void generateClearCode(MethodSpec.Builder method) {

        }

        @Override
        public void generateCopyFromCode(MethodSpec.Builder method) {

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
        public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {

        }

        @Override
        public void generateEqualsCode(MethodSpec.Builder method) {

        }

    }

}
