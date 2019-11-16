/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
        assertEquals("protos.test.java.UnittestRequired.SimpleMessage", map.resolveMessageType(".quickbuf_unittest_import.SimpleMessage").toString());
        assertEquals("protos.test.java.UnittestRequired.TestAllTypesRequired", map.resolveMessageType(".quickbuf_unittest_import.TestAllTypesRequired").toString());
    }

    // Uses default namespaces
    @Test
    public void testImportTypeMap() {
        CodeGeneratorRequest request = TestRequestLoader.getImportRequest();
        TypeRegistry map = RequestInfo.withTypeRegistry(request).getTypeRegistry();

        assertEquals(8, map.typeMap.entrySet().size());
        assertEquals("protos.test.java.ContainerMessage", map.resolveMessageType(".quickbuf_unittest.ContainerMessage").toString());
        assertEquals("protos.test.java.ForeignEnum", map.resolveMessageType(".quickbuf_unittest.ForeignEnum").toString());

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

        assertEquals("protos.test.robo.external.ImportMessage", map.resolveMessageType(".quickbuf_unittest_import.ImportMessage").toString());
        assertEquals("protos.test.robo.external.ImportMessage.NestedImportMessage", map.resolveMessageType(".quickbuf_unittest_import.ImportMessage.NestedImportMessage").toString());
        assertEquals("protos.test.robo.ForeignEnum", map.resolveMessageType(".quickbuf_unittest.ForeignEnum").toString());
    }

}
