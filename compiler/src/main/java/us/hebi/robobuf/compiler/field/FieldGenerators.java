package us.hebi.robobuf.compiler.field;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerators {

    public static FieldGenerator createGenerator(FieldDescriptorProto descriptor) {
        return new FieldGenerator() {
            @Override
            public void generateMembers(TypeSpec.Builder type) {

            }

            @Override
            public void generateClearCode(MethodSpec.Builder method) {

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
