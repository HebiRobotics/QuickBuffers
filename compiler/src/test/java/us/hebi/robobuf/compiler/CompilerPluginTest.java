package us.hebi.robobuf.compiler;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 05 Aug 2019
 */
public class CompilerPluginTest {

    @Test
    public void testHandleRequest() throws IOException {

        InputStream is = CompilerPluginTest.class.getResourceAsStream("test/simple.request");
        assertTrue(is.available() > 0);
        CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(is);
        assertTrue(request.getSerializedSize() > 0);

    }

}