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

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class TypeRegistryTest {

    @Test
    public void testRequiredTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getRequiredRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();
        assertTrue(map.typeMap.size() >= 2);
        assertEquals("protos.test.protobuf.UnittestRequired.SimpleMessage", map.resolveMessageType(".quickbuf_unittest_import.SimpleMessage").toString());
        assertEquals("protos.test.protobuf.UnittestRequired.TestAllTypesRequired", map.resolveMessageType(".quickbuf_unittest_import.TestAllTypesRequired").toString());
        testRegistryContents("" +
                ".quickbuf_unittest_import.NestedRequiredMessage=protos.test.protobuf.UnittestRequired.NestedRequiredMessage\n" +
                ".quickbuf_unittest_import.SimpleMessage=protos.test.protobuf.UnittestRequired.SimpleMessage\n" +
                ".quickbuf_unittest_import.TestAllTypesRequired=protos.test.protobuf.UnittestRequired.TestAllTypesRequired\n" +
                ".quickbuf_unittest_import.TestAllTypesRequired.NestedEnum=protos.test.protobuf.UnittestRequired.TestAllTypesRequired.NestedEnum\n", map);
        testAllTypesResolved(request);
    }

    // Uses default namespaces
    @Test
    public void testImportTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getImportRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(12, map.typeMap.entrySet().size());
        assertEquals("protos.test.protobuf.ContainerMessage", map.resolveMessageType(".quickbuf_unittest.ContainerMessage").toString());
        assertEquals("protos.test.protobuf.ForeignEnum", map.resolveMessageType(".quickbuf_unittest.ForeignEnum").toString());

        // Make sure lookup directly by typename
        String typeId = request
                .getProtoFile(1) // namespaces_import.proto
                .getMessageType(0) // ContainerMessage
                .getField(6) // optional_nested_import_message
                .getTypeName();
        assertEquals("protos.test.protobuf.external.ImportMessage.NestedImportMessage", map.resolveMessageType(typeId).toString());

        testRegistryContents("" +
                ".ImportingMessage=protos.test.protobuf.nopackage.NamespacesDefaultPackage.ImportingMessage\n" +
                ".NoPackageImportMessage=protos.test.protobuf.nopackage.NamespacesDefaultPackage.NoPackageImportMessage\n" +
                ".NoPackageImportMessage.NoPackageNestedImportMessage=protos.test.protobuf.nopackage.NamespacesDefaultPackage.NoPackageImportMessage.NoPackageNestedImportMessage\n" +
                ".quickbuf_unittest.ContainerMessage=protos.test.protobuf.ContainerMessage\n" +
                ".quickbuf_unittest.ContainerMessage.NestedEnum=protos.test.protobuf.ContainerMessage.NestedEnum\n" +
                ".quickbuf_unittest.ContainerMessage.NestedMessage=protos.test.protobuf.ContainerMessage.NestedMessage\n" +
                ".quickbuf_unittest.ForeignEnum=protos.test.protobuf.ForeignEnum\n" +
                ".quickbuf_unittest.ForeignMessage=protos.test.protobuf.ForeignMessage\n" +
                ".quickbuf_unittest_import.ImportEnum=protos.test.protobuf.external.ImportEnum\n" +
                ".quickbuf_unittest_import.ImportMessage=protos.test.protobuf.external.ImportMessage\n" +
                ".quickbuf_unittest_import.ImportMessage.NestedImportMessage=protos.test.protobuf.external.ImportMessage.NestedImportMessage\n" +
                ".unittest_packages.ForeignDefaultPackageImport=protos.test.protobuf.nopackage.ImportingOuterClass.ForeignDefaultPackageImport\n", map);
        testAllTypesResolved(request);
    }

    // Uses renamed packages
    @Test
    public void testAllTypesTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getAllTypesEagerRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(15, map.typeMap.entrySet().size());

        assertEquals("protos.test.quickbuf.external.ImportMessage", map.resolveMessageType(".quickbuf_unittest_import.ImportMessage").toString());
        assertEquals("protos.test.quickbuf.external.ImportMessage.NestedImportMessage", map.resolveMessageType(".quickbuf_unittest_import.ImportMessage.NestedImportMessage").toString());
        assertEquals("protos.test.quickbuf.ForeignEnum", map.resolveMessageType(".quickbuf_unittest.ForeignEnum").toString());

        testRegistryContents("" +
                ".quickbuf_unittest.ForeignEnum=protos.test.quickbuf.ForeignEnum\n" +
                ".quickbuf_unittest.ForeignMessage=protos.test.quickbuf.ForeignMessage\n" +
                ".quickbuf_unittest.TestAllTypes=protos.test.quickbuf.TestAllTypes\n" +
                ".quickbuf_unittest.TestAllTypes.MessageSetCorrect=protos.test.quickbuf.TestAllTypes.MessageSetCorrect\n" +
                ".quickbuf_unittest.TestAllTypes.MessageSetCorrectExtension1=protos.test.quickbuf.TestAllTypes.MessageSetCorrectExtension1\n" +
                ".quickbuf_unittest.TestAllTypes.MessageSetCorrectExtension2=protos.test.quickbuf.TestAllTypes.MessageSetCorrectExtension2\n" +
                ".quickbuf_unittest.TestAllTypes.NestedEnum=protos.test.quickbuf.TestAllTypes.NestedEnum\n" +
                ".quickbuf_unittest.TestAllTypes.NestedMessage=protos.test.quickbuf.TestAllTypes.NestedMessage\n" +
                ".quickbuf_unittest.TestAllTypes.OptionalGroup=protos.test.quickbuf.TestAllTypes.OptionalGroup\n" +
                ".quickbuf_unittest.TestAllTypes.RepeatedGroup=protos.test.quickbuf.TestAllTypes.RepeatedGroup\n" +
                ".quickbuf_unittest.TestCommentInjectionMessage=protos.test.quickbuf.TestCommentInjectionMessage\n" +
                ".quickbuf_unittest.TestExtremeDefaultValues=protos.test.quickbuf.TestExtremeDefaultValues\n" +
                ".quickbuf_unittest_import.ImportEnum=protos.test.quickbuf.external.ImportEnum\n" +
                ".quickbuf_unittest_import.ImportMessage=protos.test.quickbuf.external.ImportMessage\n" +
                ".quickbuf_unittest_import.ImportMessage.NestedImportMessage=protos.test.quickbuf.external.ImportMessage.NestedImportMessage\n", map);
        testAllTypesResolved(request);
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
        testAllTypesResolved(request);
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
        testAllTypesResolved(request);
    }

    private static void testRegistryContents(String expected, TypeRegistry registry) {
        StringBuilder content = new StringBuilder();
        registry.typeMap.keySet().stream().sorted()
                .forEachOrdered(key -> content.append(key).append("=").append(registry.resolveMessageType(key)).append("\n"));
        assertEquals(expected, content.toString());
    }

    private static void testAllTypesResolved(CodeGeneratorRequest request) {
        testAllTypesResolved(RequestInfo.withTypeRegistry(request));
    }

    private static void testAllTypesResolved(RequestInfo request) {
        testAllTypesResolved(request.getDescriptor(), request.getTypeRegistry());
    }

    private static void testAllTypesResolved(CodeGeneratorRequest request, TypeRegistry registry) {
        for (DescriptorProtos.FileDescriptorProto files : request.getProtoFileList()) {
            testAllTypesResolved(files.getMessageTypeList(), registry);
        }
    }

    private static void testAllTypesResolved(List<DescriptorProtos.DescriptorProto> messages, TypeRegistry registry) {
        for (DescriptorProtos.DescriptorProto message : messages) {
            for (DescriptorProtos.FieldDescriptorProto field : message.getFieldList()) {
                assertNotNull(registry.resolveJavaTypeFromProto(field));
            }
            testAllTypesResolved(message.getNestedTypeList(), registry);

        }
    }

}
