package us.hebi.robobuf.compiler.test;

import com.google.protobuf.compiler.PluginProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static us.hebi.robobuf.compiler.Preconditions.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
public class TestRequestLoader {

    @Test
    public void testAllAvailable() throws IOException {
        getSimpleRequest();
        getImportRequest();
    }

    public static CodeGeneratorRequest getSimpleRequest() throws IOException {
        return getRequest("simple");
    }

    public static CodeGeneratorRequest getImportRequest() throws IOException {
        return getRequest("import");
    }

    private static CodeGeneratorRequest getRequest(String name) throws IOException {
        InputStream is = TestRequestLoader.class.getResourceAsStream(name + ".request");
        return CodeGeneratorRequest.parseFrom(checkNotNull(is));
    }

}
