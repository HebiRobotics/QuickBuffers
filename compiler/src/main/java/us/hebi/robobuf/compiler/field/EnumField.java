package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
public class EnumField extends FieldGenerator {

    protected EnumField(RequestInfo.FieldInfo info) {
        super(info);
    }

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

}
