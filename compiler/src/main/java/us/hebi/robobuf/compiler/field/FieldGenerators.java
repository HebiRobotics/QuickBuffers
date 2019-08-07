package us.hebi.robobuf.compiler.field;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.TypeMap;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerators {

    public static FieldGenerator createGenerator(FieldDescriptorProto descriptor, TypeMap typeMap, int fieldIndex) {
        switch (descriptor.getType()) {
            case TYPE_DOUBLE:
                break;
            case TYPE_FLOAT:
                break;
            case TYPE_INT64:
                break;
            case TYPE_UINT64:
                break;
            case TYPE_INT32:
                break;
            case TYPE_FIXED64:
                break;
            case TYPE_FIXED32:
                break;
            case TYPE_BOOL:
                break;
            case TYPE_STRING:
                break;
            case TYPE_GROUP:
                break;
            case TYPE_MESSAGE:
                return new MessageField(descriptor, typeMap, fieldIndex);
            case TYPE_BYTES:
                break;
            case TYPE_UINT32:
                break;
            case TYPE_ENUM:
                break;
            case TYPE_SFIXED32:
                break;
            case TYPE_SFIXED64:
                break;
            case TYPE_SINT32:
                break;
            case TYPE_SINT64:
                break;
        }

        return new FieldGenerator() {
            @Override
            public void generateMembers(TypeSpec.Builder type) {

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
            public void generateSerializedSizeCode(MethodSpec.Builder method) {

            }

            @Override
            public void generateEqualsCode(MethodSpec.Builder method) {

            }

            @Override
            public void generateHashCodeCode(MethodSpec.Builder method) {

            }
        };
    }

}
