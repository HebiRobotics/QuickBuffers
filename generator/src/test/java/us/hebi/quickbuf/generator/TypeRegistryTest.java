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
        assertEquals("protos.test.protobuf.UnittestRequired.SimpleMessage", map.resolveMessageType(".quickbuf_unittest_import.SimpleMessage").toString());
        assertEquals("protos.test.protobuf.UnittestRequired.TestAllTypesRequired", map.resolveMessageType(".quickbuf_unittest_import.TestAllTypesRequired").toString());
    }

    // Uses default namespaces
    @Test
    public void testImportTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getImportRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(8, map.typeMap.entrySet().size());
        assertEquals("protos.test.protobuf.ContainerMessage", map.resolveMessageType(".quickbuf_unittest.ContainerMessage").toString());
        assertEquals("protos.test.protobuf.ForeignEnum", map.resolveMessageType(".quickbuf_unittest.ForeignEnum").toString());

        // Make sure lookup directly by typename
        String typeId = request
                .getProtoFile(1) // namespaces_import.proto
                .getMessageType(0) // ContainerMessage
                .getField(6) // optional_nested_import_message
                .getTypeName();
        assertEquals("protos.test.protobuf.external.ImportMessage.NestedImportMessage", map.resolveMessageType(typeId).toString());

    }

    // Uses renamed packages
    @Test
    public void testAllTypesTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getAllTypesEagerRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(12, map.typeMap.entrySet().size());

        assertEquals("protos.test.quickbuf.external.ImportMessage", map.resolveMessageType(".quickbuf_unittest_import.ImportMessage").toString());
        assertEquals("protos.test.quickbuf.external.ImportMessage.NestedImportMessage", map.resolveMessageType(".quickbuf_unittest_import.ImportMessage.NestedImportMessage").toString());
        assertEquals("protos.test.quickbuf.ForeignEnum", map.resolveMessageType(".quickbuf_unittest.ForeignEnum").toString());
    }

    @Test
    public void testLazyRequiredRecursion() {
        CodeGeneratorRequest request = TestRequestLoader.getLazyRecursionRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals("protos.test.quickbuf.unsupported.Recursion.MainMessage", map.resolveMessageType(".quickbuf_unsupported.MainMessage").toString());
        assertTrue(map.hasRequiredFieldsInHierarchy(map.resolveMessageType(".quickbuf_unsupported.MainMessage")));

        assertEquals("protos.test.quickbuf.unsupported.Recursion.NestedMessage", map.resolveMessageType(".quickbuf_unsupported.NestedMessage").toString());
        assertTrue(map.hasRequiredFieldsInHierarchy(map.resolveMessageType(".quickbuf_unsupported.NestedMessage")));

        assertEquals("protos.test.quickbuf.unsupported.Recursion.InnerNestedMessage", map.resolveMessageType(".quickbuf_unsupported.InnerNestedMessage").toString());
        assertTrue(map.hasRequiredFieldsInHierarchy(map.resolveMessageType(".quickbuf_unsupported.InnerNestedMessage")));
    }

    @Test
    public void testLazyRequiredRecursion_reverse() {
        CodeGeneratorRequest request = TestRequestLoader.getLazyRecursionRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals("protos.test.quickbuf.unsupported.Recursion.InnerNestedMessage", map.resolveMessageType(".quickbuf_unsupported.InnerNestedMessage").toString());
        assertTrue(map.hasRequiredFieldsInHierarchy(map.resolveMessageType(".quickbuf_unsupported.InnerNestedMessage")));

        assertEquals("protos.test.quickbuf.unsupported.Recursion.NestedMessage", map.resolveMessageType(".quickbuf_unsupported.NestedMessage").toString());
        assertTrue(map.hasRequiredFieldsInHierarchy(map.resolveMessageType(".quickbuf_unsupported.NestedMessage")));

        assertEquals("protos.test.quickbuf.unsupported.Recursion.MainMessage", map.resolveMessageType(".quickbuf_unsupported.MainMessage").toString());
        assertTrue(map.hasRequiredFieldsInHierarchy(map.resolveMessageType(".quickbuf_unsupported.MainMessage")));
    }

}
