package us.hebi.robobuf.compiler.type;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.squareup.javapoet.*;
import us.hebi.robobuf.compiler.TypeMap;
import us.hebi.robobuf.compiler.field.BitField;
import us.hebi.robobuf.compiler.field.FieldGenerator;
import us.hebi.robobuf.compiler.field.FieldGenerators;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class MessageGenerator implements TypeGenerator {

    public MessageGenerator(DescriptorProto descriptor, boolean isNested, TypeMap typeMap) {
        this.descriptor = descriptor;
        this.isNested = isNested;
        this.thisClass = ClassName.get("", descriptor.getName());
        this.typeMap = typeMap;

        // Nested messages
        descriptor.getNestedTypeList().stream()
                .map(msg -> new MessageGenerator(msg, true, typeMap))
                .forEach(nestedTypes::add);

        // Nested enums
        descriptor.getEnumTypeList().stream()
                .map(EnumGenerator::new)
                .forEach(nestedTypes::add);

        // Fields
        for (int i = 0; i < descriptor.getFieldCount(); i++) {
            fields.add(FieldGenerators.createGenerator(descriptor.getField(i), typeMap, i));
        }

    }

    public TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.classBuilder(descriptor.getName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        if (isNested) {
            type.addModifiers(Modifier.STATIC);
        }

        if (!isNested) {
            // Note: constants from enums and fields may have the same names
            // as constants in the nested classes. This causes Java warnings,
            // but is not fatal, so we suppress those warnings in the top-most
            // class declaration /javanano
            type.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "hiding")
                    .build());
        }

        // Types
        nestedTypes.stream()
                .map(TypeGenerator::generate)
                .forEach(type::addType);

        // Fields
        descriptor.getFieldList().stream()
                .map(fieldDescriptor -> FieldGenerators.createGenerator(fieldDescriptor, typeMap, 0))
                .forEach(fieldGen -> fieldGen.generateMembers(type));

        generateCopyFrom(type);
        generateClear(type);

        // Has-state
        for (int i = 0; i < BitField.getNumberOfFields(descriptor.getFieldCount()); i++) {
            type.addField(FieldSpec.builder(int.class, BitField.fieldName(i), Modifier.PRIVATE).build());
        }

        // Internal methods
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("clear()")
                .build());
        return type.build();
    }

    private void generateClear(TypeSpec.Builder type) {
        MethodSpec.Builder clear = MethodSpec.methodBuilder("clear")
                .addModifiers(Modifier.PUBLIC)
                .returns(thisClass);
        // TODO: clear bitfields
        fields.forEach(field -> field.generateClearCode(clear));
        clear.addStatement("return this");
        type.addMethod(clear.build());
    }

    private void generateFieldInitializers(TypeSpec.Builder type) {

    }

    private void generateEquals(TypeSpec.Builder type) {

    }

    private void generateHashCode(TypeSpec.Builder type) {

    }

    private void generateCopyFrom(TypeSpec.Builder type) {
        MethodSpec.Builder copyFrom = MethodSpec.methodBuilder("copyFrom")
                .addParameter(thisClass, "other", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(thisClass);
        fields.forEach(field -> field.generateCopyFromCode(copyFrom));
        for (int i = 0; i < BitField.getNumberOfFields(fields.size()); i++) {
            copyFrom.addStatement("this.$1L = other.$1L", BitField.fieldName(i));
        }
        copyFrom.addStatement("return this"); // TODO: remember dirty bit
        type.addMethod(copyFrom.build());
    }

    final DescriptorProto descriptor;
    final boolean isNested;
    final TypeMap typeMap;
    final ClassName thisClass;
    final List<TypeGenerator> nestedTypes = new ArrayList<>();
    final List<FieldGenerator> fields = new ArrayList<>();

}
