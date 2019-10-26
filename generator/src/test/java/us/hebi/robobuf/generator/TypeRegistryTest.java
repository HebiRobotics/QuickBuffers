package us.hebi.robobuf.generator;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class TypeRegistryTest {

    @Test
    public void testRequiredTypeMap() {
        TypeRegistry map = RequestInfo.withTypeRegistry(TestRequestLoader.getRequiredRequest()).getTypeRegistry();
        assertTrue(map.typeMap.size() >= 2);
        assertEquals("protos.test.java.UnittestRequired.SimpleMessage", map.resolveMessageType(".robobuf_unittest_import.SimpleMessage").toString());
        assertEquals("protos.test.java.UnittestRequired.TestAllTypesRequired", map.resolveMessageType(".robobuf_unittest_import.TestAllTypesRequired").toString());
    }

    // Uses default namespaces
    @Test
    public void testImportTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getImportRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(8, map.typeMap.entrySet().size());
        assertEquals("protos.test.java.ContainerMessage", map.resolveMessageType(".robobuf_unittest.ContainerMessage").toString());
        assertEquals("protos.test.java.ForeignEnum", map.resolveMessageType(".robobuf_unittest.ForeignEnum").toString());

        // Make sure lookup directly by typename
        String typeId = request
                .getProtoFile(1) // namespaces_import.proto
                .getMessageType(0) // ContainerMessage
                .getField(6) // optional_nested_import_message
                .getTypeName();
        assertEquals("protos.test.java.external.ImportMessage.NestedImportMessage", map.resolveMessageType(typeId).toString());

    }

    // Uses renamed packages
    @Test
    public void testAllTypesTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getAllTypesRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(10, map.typeMap.entrySet().size());

        assertEquals("protos.test.robo.external.ImportMessage", map.resolveMessageType(".robobuf_unittest_import.ImportMessage").toString());
        assertEquals("protos.test.robo.external.ImportMessage.NestedImportMessage", map.resolveMessageType(".robobuf_unittest_import.ImportMessage.NestedImportMessage").toString());
        assertEquals("protos.test.robo.ForeignEnum", map.resolveMessageType(".robobuf_unittest.ForeignEnum").toString());
    }

}