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
        getRequiredRequest();
        getImportRequest();
        getAllTypesRequest();
        getRepeatedPackablesRequest();
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

    private static CodeGeneratorRequest getRequest(String name) {
        try {
            InputStream is = TestRequestLoader.class.getResourceAsStream(name + ".request");
            return CodeGeneratorRequest.parseFrom(checkNotNull(is));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
