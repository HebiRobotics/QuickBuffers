package us.hebi.robobuf.compiler.test;

import com.google.protobuf.compiler.PluginProtos;

import java.io.IOException;
import java.io.InputStream;

import static us.hebi.robobuf.compiler.Preconditions.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
public class TestRequestLoader {

    public static PluginProtos.CodeGeneratorRequest getSimpleRequest() throws IOException {
        return getRequest("simple");
    }

    public static PluginProtos.CodeGeneratorRequest getImportRequest() throws IOException {
        return getRequest("import");
    }

    private static PluginProtos.CodeGeneratorRequest getRequest(String name) throws IOException {
        InputStream is = TestRequestLoader.class.getResourceAsStream(name + ".request");
        return PluginProtos.CodeGeneratorRequest.parseFrom(checkNotNull(is));
    }

}
