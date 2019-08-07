package us.hebi.robobuf.compiler;

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
    public void testAllAvailable() {
        getSimpleRequest();
        getImportRequest();
    }

    public static CodeGeneratorRequest getSimpleRequest() {
        return getRequest("simple");
    }

    public static CodeGeneratorRequest getImportRequest() {
        return getRequest("import");
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
