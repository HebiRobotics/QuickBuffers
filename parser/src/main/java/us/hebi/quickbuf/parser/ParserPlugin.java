/*-
 * #%L
 * quickbuf-parser
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.quickbuf.parser;

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
