package us.hebi.robobuf.compiler.type;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.squareup.javapoet.TypeSpec;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
@RequiredArgsConstructor
public class EnumGenerator implements TypeGenerator {

    public TypeSpec generate(boolean nested) {
        TypeSpec.Builder type = TypeSpec.enumBuilder(descriptor.getName())
                .addModifiers(Modifier.PUBLIC);

        for (EnumValueDescriptorProto value : descriptor.getValueList()) {
            type.addEnumConstant(value.getName());
        }

        return type.build();
    }

    final DescriptorProtos.EnumDescriptorProto descriptor;

}
