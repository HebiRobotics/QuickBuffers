package us.hebi.robobuf.compiler.type;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo.MessageInfo;
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

    public MessageGenerator(MessageInfo info) {
        this.info = info;
        info.getNestedTypes().forEach(t -> nestedTypes.add(new MessageGenerator(t)));
        info.getNestedEnums().forEach(t -> nestedTypes.add(new EnumGenerator(t)));
        info.getFields().forEach(f -> fields.add(FieldGenerators.createGenerator(f)));
    }

    public TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.classBuilder(info.getTypeName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        if (info.isNested()) {
            type.addModifiers(Modifier.STATIC);
        }

        if (!info.isNested()) {
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
        fields.forEach(f-> f.generateMembers(type));
        generateCopyFrom(type);
        generateClear(type);

        // Has-state
        for (int i = 0; i < BitField.getNumberOfFields(info.getFieldCount()); i++) {
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
                .returns(info.getTypeName());
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
                .addParameter(info.getTypeName(), "other", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());
        fields.forEach(field -> field.generateCopyFromCode(copyFrom));
        for (int i = 0; i < BitField.getNumberOfFields(fields.size()); i++) {
            copyFrom.addStatement("this.$1L = other.$1L", BitField.fieldName(i));
        }
        copyFrom.addStatement("return this"); // TODO: remember dirty bit
        type.addMethod(copyFrom.build());
    }

    final MessageInfo info;
    final List<TypeGenerator> nestedTypes = new ArrayList<>();
    final List<FieldGenerator> fields = new ArrayList<>();

}
