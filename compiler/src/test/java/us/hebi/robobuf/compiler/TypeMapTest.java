package us.hebi.robobuf.compiler;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class TypeMapTest {

    @Test
    public void testSimpleTypeMap() {
        TypeMap map = TypeMap.fromRequest(TestRequestLoader.getSimpleRequest());
        assertEquals(1, map.getMap().entrySet().size());
        assertEquals("us.hebi.robobuf.test.Simple.SimpleMessage", map.getClassName(".robobuf_unittest_import.SimpleMessage").toString());
    }

    @Test
    public void testImportTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getImportRequest();
        TypeMap map = TypeMap.fromRequest(request);

        assertEquals(8, map.getMap().entrySet().size());
        assertEquals("us.hebi.robobuf.test.ContainerMessage", map.getClassName(".robobuf_unittest.ContainerMessage").toString());
        assertEquals("us.hebi.robobuf.test.ForeignEnum", map.getClassName(".robobuf_unittest.ForeignEnum").toString());

        // Make sure lookup directly by typename
        String typeId = request
                .getProtoFile(1) // namespaces_import.proto
                .getMessageType(0) // ContainerMessage
                .getField(6) // optional_nested_import_message
                .getTypeName();
        assertEquals("us.hebi.robobuf.test.external.ImportMessage.NestedImportMessage", map.getClassName(typeId).toString());

    }

}