package us.hebi.robobuf.compiler;

import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.type.EnumGenerator;
import us.hebi.robobuf.compiler.type.MessageGenerator;
import us.hebi.robobuf.parser.ParserUtil;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Florian Enner
 * @since 05 Aug 2019
 */
public class CompilerPlugin {

    /**
     * The protoc-gen-plugin communicates via proto messages on System.in/.out
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        handleRequest(System.in).writeTo(System.out);
    }

    public static CodeGeneratorResponse handleRequest(InputStream input) throws IOException {
        try {

            // Compile files
            CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(input);
            return handleRequest(request);

        } catch (GeneratorException ge) {
            return ParserUtil.asError(ge.getMessage());
        } catch (Exception ex) {
            return ParserUtil.asErrorWithStackTrace(ex);
        }

    }

    public static CodeGeneratorResponse handleRequest(CodeGeneratorRequest request) throws IOException {
        CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
        Map<String, String> generatorParams = ParserUtil.getGeneratorParameters(request);
        TypeMap typeMap = TypeMap.fromRequest(request);

        for (FileDescriptorProto fileDescriptors : request.getProtoFileList()) {
            FileParams params = new FileParams(generatorParams, fileDescriptors);
            boolean nested = !params.isGenerateMultipleFiles();
            ClassName outerClassName = ClassName.get(params.getJavaPackage(), params.getOuterClassname());
            TypeSpec.Builder outerClassSpec = TypeSpec.classBuilder(outerClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            List<TypeSpec> outputFiles = Lists.newArrayList();

            // Enum types
            for (EnumDescriptorProto enumType : fileDescriptors.getEnumTypeList()) {
                TypeSpec typeSpec = new EnumGenerator(enumType).generate();
                if (nested) outerClassSpec.addType(typeSpec);
                else outputFiles.add(typeSpec);
            }

            // Message types
            for (DescriptorProto messageType : fileDescriptors.getMessageTypeList()) {
                TypeSpec typeSpec = new MessageGenerator(messageType, nested, typeMap).generate();
                if (nested) outerClassSpec.addType(typeSpec);
                else outputFiles.add(typeSpec);
            }

            // Generate Java files
            outputFiles.add(outerClassSpec.build());
            String directory = "";
            if(!outerClassName.packageName().isEmpty())
                directory = outerClassName.packageName().replaceAll("\\.", "/") + "/";

            for (TypeSpec typeSpec : outputFiles) {

                JavaFile javaFile = JavaFile.builder(params.getJavaPackage(), typeSpec)
                        .indent("    ")
                        .skipJavaLangImports(true)
                        .build();

                StringBuilder content = new StringBuilder(1000);
                javaFile.writeTo(content);

                response.addFile(CodeGeneratorResponse.File.newBuilder()
                        .setName(directory + typeSpec.name + ".java")
                        .setContent(content.toString())
                        .build());
            }

        }

        return response.build();

    }

}
