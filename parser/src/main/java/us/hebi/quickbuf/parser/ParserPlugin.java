/*-
 * #%L
 * quickbuf-parser
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

package us.hebi.quickbuf.parser;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature;

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

        // Signal proto3 optional support bit
        // see https://github.com/protocolbuffers/protobuf/blob/f75fd051d68136ce366c464cea4f3074158cd141/docs/implementing_proto3_presence.md#signaling-that-your-code-generator-supports-proto3-optional
        response.setSupportedFeatures(Feature.FEATURE_PROTO3_OPTIONAL_VALUE);

        // Figure out file name
        Map<String, String> parameters = ParserUtil.getGeneratorParameters(request);
        String fileName = parameters.get("request_file");
        if (fileName == null)
            return ParserUtil.asError("parameter 'request_file' is not set");

        // Add content
        response.addFile(CodeGeneratorResponse.File.newBuilder()
                .setContentBytes(request.toByteString())
                .setName(fileName));

        return response.build();

    }

}
