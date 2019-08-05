package us.hebi.robobuf.compiler;

import com.google.protobuf.compiler.PluginProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Florian Enner
 * @since 05 Aug 2019
 */
public class CompilerPluginTest {

    @Test
    public void testHandleRequest() throws IOException {

        InputStream is = CompilerPluginTest.class.getResourceAsStream("proto/simple.desc");
        CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(is);

    }

}