package us.hebi.robobuf.parser;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Florian Enner
 * @since 05 Aug 2019
 */
public class ParserPlugin {

    /**
     * A protoc-gen-plugin that communicates with protoc via messages on System.in and System.out
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

        } catch (Exception e) {
            return ParserUtil.asErrorWithStackTrace(e);
        }
    }

    public static CodeGeneratorResponse handleRequest(CodeGeneratorRequest request) throws IOException {
        CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();

        // Figure out file name
        Map<String, String> parameters = ParserUtil.getGeneratorParameters(request);
        String fileName = parameters.get("requestFile");
        if (fileName == null)
            return ParserUtil.asError("parameter 'requestFile' is not set");

        // Add content
        response.addFile(CodeGeneratorResponse.File.newBuilder()
                .setContentBytes(request.toByteString())
                .setName(fileName));

        return response.build();

    }

}
