package us.hebi.robobuf.compiler;

import com.google.common.collect.Lists;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo.FileInfo;
import us.hebi.robobuf.compiler.type.EnumGenerator;
import us.hebi.robobuf.compiler.type.MessageGenerator;
import us.hebi.robobuf.parser.ParserUtil;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

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

    public static CodeGeneratorResponse handleRequest(CodeGeneratorRequest requestProto) {
        CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
        RequestInfo request = RequestInfo.withTypeMap(requestProto);

        for (FileInfo file : request.getFiles()) {

            // Generate type specifications
            List<TypeSpec> topLevelTypes = Lists.newArrayList();
            TypeSpec.Builder outerClassSpec = TypeSpec.classBuilder(file.getOuterClassName())
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            Consumer<TypeSpec> list = file.isGenerateMultipleFiles() ? topLevelTypes::add : outerClassSpec::addType;

            for (RequestInfo.EnumInfo type : file.getEnumTypes()) {
                list.accept(new EnumGenerator(type).generate());
            }

            for (RequestInfo.MessageInfo type : file.getMessageTypes()) {
                list.accept(new MessageGenerator(type).generate());
            }

            topLevelTypes.add(outerClassSpec.build());

            // Generate Java files
            for (TypeSpec typeSpec : topLevelTypes) {

                JavaFile javaFile = JavaFile.builder(file.getJavaPackage(), typeSpec)
                        .indent(request.getIndentString())
                        .skipJavaLangImports(true)
                        .build();

                StringBuilder content = new StringBuilder(1000);
                try {
                    javaFile.writeTo(content);
                } catch (IOException e) {
                    throw new AssertionError("Could not write to StringBuilder?");
                }

                response.addFile(CodeGeneratorResponse.File.newBuilder()
                        .setName(file.getOutputDirectory() + typeSpec.name + ".java")
                        .setContent(content.toString())
                        .build());
            }

        }

        return response.build();

    }

}
