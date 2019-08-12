package us.hebi.robobuf.compiler.type;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.BitField;
import us.hebi.robobuf.compiler.RequestInfo.MessageInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;
import us.hebi.robobuf.compiler.field.FieldGenerator;
import us.hebi.robobuf.compiler.field.FieldGenerators;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class MessageGenerator implements TypeGenerator {

    public MessageGenerator(MessageInfo info) {
        this.info = info;
        info.getNestedEnums().forEach(t -> nestedTypes.add(new EnumGenerator(t)));
        info.getNestedTypes().forEach(t -> nestedTypes.add(new MessageGenerator(t)));
        info.getFields().forEach(f -> fields.add(FieldGenerators.createGenerator(f)));
    }

    public TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.classBuilder(info.getTypeName())
                .superclass(RuntimeClasses.BASE_MESSAGE)
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

        // Member state
        for (int i = 0; i < BitField.getNumberOfFields(info.getFieldCount()); i++) {
            type.addField(FieldSpec.builder(int.class, BitField.fieldName(i), Modifier.PRIVATE).build());
        }
        fields.forEach(f -> f.generateMemberFields(type));

        // Fields accessors
        fields.forEach(f -> f.generateMemberMethods(type));
        generateCopyFrom(type);
        generateClear(type);

        // Internal Constructor
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("clear()")
                .build());

        generateWriteTo(type);
        generateComputeSerializedSize(type);
        generateMergeFrom(type);
        generateEquals(type);
        generateHashCode(type);

        return type.build();
    }

    private void generateClear(TypeSpec.Builder type) {
        MethodSpec.Builder clear = MethodSpec.methodBuilder("clear")
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());
        for (int i = 0; i < BitField.getNumberOfFields(fields.size()); i++) {
            clear.addStatement("$L = 0", BitField.fieldName(0));
        }
        fields.forEach(field -> field.generateClearCode(clear));
        clear.addStatement("return this");
        type.addMethod(clear.build());
    }

    private void generateEquals(TypeSpec.Builder type) {
        MethodSpec.Builder equals = MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "o");

        // Reference equality check
        equals.beginControlFlow("if (o == this)")
                .addStatement("return true")
                .endControlFlow();

        // Type check
        equals.beginControlFlow("if (!(o instanceof $T))", info.getTypeName())
                .addStatement("return false")
                .endControlFlow();
        equals.addStatement("$1T other = ($1T) o", info.getTypeName());

        // Check whether all of the same fields are set
        if (info.getFieldCount() > 0) {
            equals.addCode("return $1L == other.$1L$>", BitField.fieldName(0));
            for (int i = 1; i < BitField.getNumberOfFields(info.getFieldCount()); i++) {
                equals.addCode("\n&& $1L == other.$1L", BitField.fieldName(i));
            }

            for (FieldGenerator field : fields) {
                equals.addCode("\n&& (!$1N() || ", field.getInfo().getHazzerName());
                field.generateEqualsStatement(equals);
                equals.addCode(")");
            }

            equals.addCode(";$<\n");
        } else {
            equals.addCode("return true");
        }

        type.addMethod(equals.build());
    }

    private void generateHashCode(TypeSpec.Builder type) {
        MethodSpec.Builder hashCode = MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);
        hashCode.addJavadoc("" +
                "Messages have no immutable state and should not\n" +
                "be used in hashing structures. This implementation\n" +
                "returns a constant value in order to satisfy the\n" +
                "contract.\n");
        hashCode.addStatement("return 0");
        type.addMethod(hashCode.build());
    }

    private void generateMergeFrom(TypeSpec.Builder type) {
        MethodSpec.Builder mergeFrom = MethodSpec.methodBuilder("mergeFrom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addParameter(RuntimeClasses.PROTO_SOURCE, "input")
                .addException(IOException.class);

        mergeFrom.beginControlFlow("while (true)");
        mergeFrom.addStatement("int tag = input.readTag()");
        mergeFrom.beginControlFlow("switch (tag)");

        // zero means invalid tag / end of data
        mergeFrom.beginControlFlow("case 0:")
                .addStatement("return this")
                .endControlFlow();

        // default case -> skip field
        mergeFrom.beginControlFlow("default:")
                .beginControlFlow("if (!input.skipField(tag))")
                .addStatement("return this")
                .endControlFlow()
                .endControlFlow();

        // individual fields
        fields.forEach(field -> {
            // Non-packed
            mergeFrom.beginControlFlow("case $L:", field.getInfo().getTag());
            field.generateMergingCode(mergeFrom);
            mergeFrom.addStatement("break");
            mergeFrom.endControlFlow();

            // Packed code (needs to be generated even for non packed fields for forwards compatibility)
            if (field.getInfo().isPackable()) {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getPackedTag());
                field.generateMergingCodeFromPacked(mergeFrom);
                mergeFrom.addStatement("break");
                mergeFrom.endControlFlow();
            }
        });

        mergeFrom.endControlFlow();
        mergeFrom.endControlFlow();
        type.addMethod(mergeFrom.build());
    }

    private void generateWriteTo(TypeSpec.Builder type) {
        MethodSpec.Builder writeTo = MethodSpec.methodBuilder("writeTo")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(RuntimeClasses.PROTO_DEST, "output")
                .addException(IOException.class);
        fields.forEach(f -> {
            writeTo.beginControlFlow("if " + f.getInfo().getHasBit());
            f.generateSerializationCode(writeTo);
            writeTo.endControlFlow();
        });
        type.addMethod(writeTo.build());
    }

    private void generateComputeSerializedSize(TypeSpec.Builder type) {
        MethodSpec.Builder computeSerializedSize = MethodSpec.methodBuilder("computeSerializedSize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(int.class);
        computeSerializedSize.addStatement("int size = 0");
        fields.forEach(f -> {
            computeSerializedSize.beginControlFlow("if " + f.getInfo().getHasBit());
            f.generateComputeSerializedSizeCode(computeSerializedSize);
            computeSerializedSize.endControlFlow();
        });
        computeSerializedSize.addStatement("return size");
        type.addMethod(computeSerializedSize.build());
    }

    private void generateCopyFrom(TypeSpec.Builder type) {
        MethodSpec.Builder copyFrom = MethodSpec.methodBuilder("copyFrom")
                .addParameter(info.getTypeName(), "other", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());
        for (int i = 0; i < BitField.getNumberOfFields(fields.size()); i++) {
            copyFrom.addStatement("$1L = other.$1L", BitField.fieldName(i));
        }
        fields.forEach(field -> field.generateCopyFromCode(copyFrom));
        copyFrom.addStatement("return this"); // TODO: remember dirty bit
        type.addMethod(copyFrom.build());
    }

    final MessageInfo info;
    final List<TypeGenerator> nestedTypes = new ArrayList<>();
    final List<FieldGenerator> fields = new ArrayList<>();

}
