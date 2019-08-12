package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.GeneratorException;
import us.hebi.robobuf.compiler.RequestInfo.FieldInfo;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerators {

    public static FieldGenerator createGenerator(FieldInfo field) {
        if (field.isRepeated()) {
            return new RepeatedField(field);
        }

        switch (field.getDescriptor().getType()) {

            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_INT64:
            case TYPE_UINT64:
            case TYPE_INT32:
            case TYPE_FIXED64:
            case TYPE_FIXED32:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SFIXED64:
            case TYPE_SINT32:
            case TYPE_SINT64:
            case TYPE_BOOL:
                return new PrimitiveField(field);

            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return new MessageField(field);

            case TYPE_ENUM:
                return new EnumField(field);

            case TYPE_STRING:
                return new StringField(field);

            case TYPE_BYTES:
                return new BytesField(field);

        }

        throw new GeneratorException("Unsupported field:\n" + field.getDescriptor());
    }

    static final class IgnoredFieldGenerator extends FieldGenerator {

        protected IgnoredFieldGenerator(FieldInfo info) {
            super(info);
        }

        @Override
        public void generateMemberFields(TypeSpec.Builder type) {
            type.addField(FieldSpec.builder(info.getTypeName(), info.getFieldName()).addJavadoc("Unsupported:\n$L\n", info.getDescriptor()).build());
        }

        @Override
        protected void generateSetter(TypeSpec.Builder type) {

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
        public void generateSerializationCode(MethodSpec.Builder method) {

        }

        @Override
        public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {

        }

    }

}
