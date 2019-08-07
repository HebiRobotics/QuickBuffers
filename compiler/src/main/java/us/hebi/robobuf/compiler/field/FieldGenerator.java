package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public interface FieldGenerator {

    void generateMembers(TypeSpec.Builder type);

    void generateClearCode(MethodSpec.Builder method);

    void generateCopyFromCode(MethodSpec.Builder method);

    void generateMergingCode(MethodSpec.Builder method);

    void generateMergingCodeFromPacked(MethodSpec.Builder method);

    void generateSerializationCode(MethodSpec.Builder method);

    void generateSerializedSizeCode(MethodSpec.Builder method);

    void generateEqualsCode(MethodSpec.Builder method);

    void generateHashCodeCode(MethodSpec.Builder method);

}
