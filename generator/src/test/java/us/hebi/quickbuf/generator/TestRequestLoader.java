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

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static us.hebi.quickbuf.generator.Preconditions.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
public class TestRequestLoader {

    @Test
    public void testAllAvailable() {
        getRequiredRequest();
        getImportRequest();
        getAllTypesRequest();
        getRepeatedPackablesRequest();
        getUnsupportedMapRequest();
        getUnsupportedExtensionRequest();
        getUnsupportedRecursionRequest();
        getUnsupportedProto3Request();
    }

    public static CodeGeneratorRequest getRequiredRequest() {
        return getRequest("required");
    }

    public static CodeGeneratorRequest getImportRequest() {
        return getRequest("import");
    }

    public static CodeGeneratorRequest getAllTypesRequest() {
        return getRequest("allTypes");
    }

    public static CodeGeneratorRequest getRepeatedPackablesRequest() {
        return getRequest("repeatedPackables");
    }

    public static CodeGeneratorRequest getUnsupportedMapRequest() {
        return getRequest("unsupported_map");
    }

    public static CodeGeneratorRequest getUnsupportedExtensionRequest() {
        return getRequest("unsupported_extension");
    }

    public static CodeGeneratorRequest getUnsupportedRecursionRequest() {
        return getRequest("unsupported_recursion");
    }

    public static CodeGeneratorRequest getUnsupportedProto3Request() {
        return getRequest("unsupported_proto3");
    }

    private static CodeGeneratorRequest getRequest(String name) {
        try {
            InputStream is = TestRequestLoader.class.getResourceAsStream(name + ".request");
            return CodeGeneratorRequest.parseFrom(checkNotNull(is));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
