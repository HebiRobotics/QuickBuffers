/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.quickbuf.generator;

import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.squareup.javapoet.*;
import us.hebi.quickbuf.generator.RequestInfo.EnumInfo;
import us.hebi.quickbuf.generator.RequestInfo.EnumValueInfo;

import javax.lang.model.element.Modifier;
import java.util.function.IntConsumer;

import static javax.lang.model.element.Modifier.*;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class EnumGenerator {

    EnumGenerator(EnumInfo info) {
        this.info = info;
        this.converterClass = info.getTypeName().nestedClass(info.getTypeName().simpleName() + "Converter");
        this.converterInterface = ParameterizedTypeName.get(RuntimeClasses.EnumConverter, info.getTypeName());
    }

    private static String getFieldName(EnumValueInfo value) {
        return NamingUtil.filterKeyword(value.getName());
    }

    private static String getValueFieldName(EnumValueInfo value) {
        return value.getName() + "_VALUE";
    }

    TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.enumBuilder(info.getTypeName())
                .addJavadoc(JavadocSpec.forEnum(info))
                .addSuperinterface(ParameterizedTypeName.get(RuntimeClasses.ProtoEnum, info.getTypeName()))
                .addModifiers(PUBLIC);

        // Add enum constants
        for (EnumValueInfo value : info.getValues()) {
            String name = value.getName();
            type.addEnumConstant(getFieldName(value),
                    TypeSpec.anonymousClassBuilder("$S, $L", name, value.getNumber())
                            .addJavadoc(JavadocSpec.forEnumValue(value)).build());
            type.addField(FieldSpec.builder(int.class, getValueFieldName(value), PUBLIC, STATIC, FINAL)
                    .initializer("$L", value.getNumber())
                    .addJavadoc(JavadocSpec.forEnumValue(value))
                    .build());
        }

        // Add alias constants
        for (EnumValueInfo alias : info.getAliases()) {
            EnumValueInfo value = info.findAliasedValue(alias);
            type.addField(FieldSpec.builder(info.getTypeName(), getFieldName(alias), PUBLIC, STATIC, FINAL)
                    .initializer("$L", getFieldName(value))
                    .addJavadoc(JavadocSpec.forEnumValue(value))
                    .build());
            type.addField(FieldSpec.builder(int.class, getValueFieldName(alias), PUBLIC, STATIC, FINAL)
                    .initializer("$L", getValueFieldName(value))
                    .addJavadoc(JavadocSpec.forEnumValue(value))
                    .build());
        }

        generateProtoEnumInterface(type);
        generateConstructor(type);
        generateStaticMethods(type);
        generateConverter(type);
        return type.build();
    }

    private void generateConstructor(TypeSpec.Builder typeSpec) {
        typeSpec.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "name")
                .addParameter(int.class, "number")
                .addStatement("this.$1N = $1N", "name")
                .addStatement("this.$1N = $1N", "number")
                .build());
        typeSpec.addField(FieldSpec.builder(String.class, "name", Modifier.PRIVATE, FINAL).build());
        typeSpec.addField(FieldSpec.builder(int.class, "number", Modifier.PRIVATE, FINAL).build());
    }

    private void generateProtoEnumInterface(TypeSpec.Builder typeSpec) {
        typeSpec.addMethod(MethodSpec.methodBuilder("getName")
                .addJavadoc("@return the string representation of enum entry")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(String.class)
                .addStatement("return name")
                .build());

        typeSpec.addMethod(MethodSpec.methodBuilder("getNumber")
                .addJavadoc("@return the numeric wire value of this enum entry")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return number")
                .build());
    }

    private void generateStaticMethods(TypeSpec.Builder typeSpec) {

        typeSpec.addMethod(MethodSpec.methodBuilder("converter")
                .addJavadoc("@return a converter that maps between this enum's numeric and text representations")
                .addModifiers(PUBLIC, STATIC)
                .returns(converterInterface)
                .addStatement("return $T.INSTANCE", converterClass)
                .build());

        typeSpec.addMethod(MethodSpec.methodBuilder("forNumber")
                .addJavadoc(
                        "@param value The numeric wire value of the corresponding enum entry.\n" +
                        "@return The enum associated with the given numeric wire value, or null if unknown.")
                .addModifiers(PUBLIC, STATIC)
                .returns(info.getTypeName())
                .addParameter(TypeName.INT, "value")
                .addStatement("return $T.INSTANCE.forNumber(value)", converterClass)
                .build());

        typeSpec.addMethod(MethodSpec.methodBuilder("forNumberOr")
                .addJavadoc("" +
                        "@param value The numeric wire value of the corresponding enum entry.\n" +
                        "@param other Fallback value in case the value is not known.\n" +
                        "@return The enum associated with the given numeric wire value, or the fallback value if unknown.")
                .addModifiers(PUBLIC, STATIC)
                .returns(info.getTypeName())
                .addParameter(int.class, "number")
                .addParameter(info.getTypeName(), "other")
                .addStatement("$T value = forNumber(number)", info.getTypeName())
                .addStatement("return value == null ? other : value")
                .build());

    }

    private void generateConverter(TypeSpec.Builder typeSpec) {
        TypeSpec.Builder decoder = TypeSpec.enumBuilder(converterClass)
                .addSuperinterface(converterInterface)
                .addEnumConstant("INSTANCE");

        // Number to Enum
        MethodSpec.Builder forNumber = MethodSpec.methodBuilder("forNumber")
                .addJavadoc(JavadocSpec.inherit())
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC, FINAL)
                .returns(info.getTypeName())
                .addParameter(TypeName.INT, "value", FINAL);

        if (info.isUsingArrayLookup()) {

            // (fast) lookup using array index
            forNumber.beginControlFlow("if (value >= 0 && value < lookup.length)")
                    .addStatement("return lookup[value]")
                    .endControlFlow()
                    .addStatement("return null");

            TypeName arrayType = ArrayTypeName.of(info.getTypeName());
            decoder.addField(FieldSpec.builder(arrayType, "lookup", Modifier.PRIVATE, STATIC, FINAL)
                    .initializer("new $T[$L]", info.getTypeName(), info.getHighestNumber() + 1)
                    .build());

            CodeBlock.Builder initBlock = CodeBlock.builder();
            for (EnumValueInfo value : info.getValues()) {
                initBlock.addStatement("lookup[$L] = $L", value.getNumber(), NamingUtil.filterKeyword(value.getName()));
            }
            decoder.addStaticBlock(initBlock.build());

        } else {

            // lookup using switch statement
            forNumber.beginControlFlow("switch(value)");
            for (EnumValueInfo value : info.getValues()) {
                forNumber.addStatement("case $L: return $L", value.getNumber(), NamingUtil.filterKeyword(value.getName()));
            }
            forNumber.addStatement("default: return null");
            forNumber.endControlFlow();

        }
        decoder.addMethod(forNumber.build());

        // Name to Enum
        MethodSpec.Builder forName = MethodSpec.methodBuilder("forName")
                .addJavadoc(JavadocSpec.inherit())
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC, FINAL)
                .returns(info.getTypeName())
                .addParameter(CharSequence.class, "value", FINAL);

        int[] cases = info.getValues().stream()
                .map(EnumValueInfo::getName)
                .mapToInt(String::length)
                .distinct()
                .sorted().toArray();

        IntConsumer createCaseIfs = len -> {
            info.getValues().stream()
                    .filter(value -> value.getName().length() == len)
                    .forEach(value -> {
                        forName.beginControlFlow("if ($T.isEqual($S, value))",
                                        RuntimeClasses.ProtoUtil, value.getName())
                                .addStatement("return $N", NamingUtil.filterKeyword(value.getName()))
                                .endControlFlow();
                    });
        };

        if (cases.length <= 3) {

            // check length brackets
            for (int len : cases) {
                forName.beginControlFlow("if (value.length() == $L)", len);
                createCaseIfs.accept(len);
                forName.endControlFlow();
            }

        } else {

            // switch lookup on length
            forName.beginControlFlow("switch (value.length())");
            for (int len : cases) {
                forName.beginControlFlow("case $L:", len);
                createCaseIfs.accept(len);
                forName
                        .addStatement("break")
                        .endControlFlow();
            }
            forName.endControlFlow();

        }

        forName.addStatement("return null");

        decoder.addMethod(forName.build());
        typeSpec.addType(decoder.build());
    }

    final EnumInfo info;
    final ClassName converterClass;
    final ParameterizedTypeName converterInterface;

}
