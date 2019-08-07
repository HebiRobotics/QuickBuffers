package us.hebi.robobuf.compiler.type;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.squareup.javapoet.TypeSpec;
import lombok.RequiredArgsConstructor;
import us.hebi.robobuf.compiler.field.FieldGenerators;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
@RequiredArgsConstructor
public class MessageGenerator implements TypeGenerator {

    public TypeSpec generate(boolean nested) {
        TypeSpec.Builder type = TypeSpec.classBuilder(descriptor.getName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (nested)
            type.addModifiers(Modifier.STATIC);

        // Nested messages
        descriptor.getNestedTypeList().stream()
                .map(MessageGenerator::new)
                .map(messageGenerator -> messageGenerator.generate(true))
                .forEach(type::addType);

        // Nested enums
        descriptor.getEnumTypeList().stream()
                .map(EnumGenerator::new)
                .map(enumGenerator -> enumGenerator.generate(true))
                .forEach(type::addType);

        // Fields
        descriptor.getFieldList().stream()
                .map(FieldGenerators::createGenerator)
                .forEach(fieldGen -> fieldGen.generateMembers(type));

        return type.build();
    }

    final DescriptorProto descriptor;
}
