/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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
import com.google.protobuf.compiler.PluginProtos;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 16 Jun 2023
 */
public class SourceLocationTests {

    @Test
    public void testComments() {
        PluginProtos.CodeGeneratorRequest request = TestRequestLoader.getAllTypesEagerRequest();
        DescriptorProtos.FileDescriptorProto descriptor = request.getProtoFileList().stream()
                .filter(f -> f.getName().equals("unittest_all_types.proto"))
                .findFirst().orElseThrow(() -> new IllegalStateException("file not found"));

        Map<String, DescriptorProtos.SourceCodeInfo.Location> map = SourceLocations.createElementMap(descriptor);
        // file.getSourceMap().forEach((key, value) -> System.out.println(key));

        // Message
        assertEquals(" comment on message header line 1\n comment on message header line 2\n", map.get(".quickbuf_unittest.TestAllTypes").getLeadingComments());
        assertEquals(" comment post message\n", map.get(".quickbuf_unittest.TestAllTypes").getTrailingComments());

        // Nested message
        assertEquals(" comment on nested message\n", map.get(".quickbuf_unittest.TestAllTypes.NestedMessage").getLeadingComments());
        assertEquals("", map.get(".quickbuf_unittest.TestAllTypes.NestedMessage").getTrailingComments());

        // Nested group
        assertEquals("", map.get(".quickbuf_unittest.TestAllTypes.OptionalGroup").getLeadingComments());
        assertEquals(" comment post group\n", map.get(".quickbuf_unittest.TestAllTypes.OptionalGroup").getTrailingComments());

        // Message Field
        assertEquals(" comment before message field\n", map.get(".quickbuf_unittest.TestAllTypes.NestedMessage.bb").getLeadingComments());
        assertEquals(" comment post message field\n", map.get(".quickbuf_unittest.TestAllTypes.NestedMessage.bb").getTrailingComments());

        // Nested Enum
        assertEquals(" comment on nested enum line 1\n\n comment on nested enum line 3\n", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum").getLeadingComments());
        assertEquals(" comment post enum\n", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum").getTrailingComments());

        // Enum Field
        assertEquals(" comment before enum field\n", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum.FOO").getLeadingComments());
        assertEquals("", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum.FOO").getTrailingComments());
        assertEquals("", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum.BAR").getLeadingComments());
        assertEquals(" comment post enum field\n", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum.BAR").getTrailingComments());
        assertEquals(" comment before enum field 2\n", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum.BAZ").getLeadingComments());
        assertEquals(" comment post enum field 2\n", map.get(".quickbuf_unittest.TestAllTypes.NestedEnum.BAZ").getTrailingComments());

    }


}
