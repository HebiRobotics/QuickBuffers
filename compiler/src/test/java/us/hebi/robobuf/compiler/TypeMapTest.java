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
        assertEquals("us.hebi.robobuf.java.Simple.SimpleMessage", map.resolveClassName(".robobuf_unittest_import.SimpleMessage").toString());
    }

    @Test
    public void testImportTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getImportRequest();
        TypeMap map = TypeMap.fromRequest(request);

        assertEquals(8, map.getMap().entrySet().size());
        assertEquals("us.hebi.robobuf.java.ContainerMessage", map.resolveClassName(".robobuf_unittest.ContainerMessage").toString());
        assertEquals("us.hebi.robobuf.java.ForeignEnum", map.resolveClassName(".robobuf_unittest.ForeignEnum").toString());

        // Make sure lookup directly by typename
        String typeId = request
                .getProtoFile(1) // namespaces_import.proto
                .getMessageType(0) // ContainerMessage
                .getField(6) // optional_nested_import_message
                .getTypeName();
        assertEquals("us.hebi.robobuf.java.external.ImportMessage.NestedImportMessage", map.resolveClassName(typeId).toString());

    }

    @Test
    public void testAllTypesTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getAllTypesRequest();
        TypeMap map = TypeMap.fromRequest(request);

        assertEquals(10, map.getMap().entrySet().size());

        map.getMap().forEach((key, value) -> System.out.println(key + " = " + value));

        assertEquals("us.hebi.robobuf.test.external.ImportMessage", map.resolveClassName(".robobuf_unittest_import.ImportMessage").toString());
        assertEquals("us.hebi.robobuf.test.AllTypesOuterClass.ForeignEnum", map.resolveClassName(".robobuf_unittest.ForeignEnum").toString());
        assertEquals("us.hebi.robobuf.test.external.ImportMessage.NestedImportMessage", map.resolveClassName(".robobuf_unittest_import.ImportMessage.NestedImportMessage").toString());
    }

}