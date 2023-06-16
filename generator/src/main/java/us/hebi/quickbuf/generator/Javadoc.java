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
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.squareup.javapoet.CodeBlock;

import java.util.Locale;

/**
 * Utilities for creating Javadoc comments on methods and fields.
 * For the most part similar to Protobuf-Java.
 *
 * @author Florian Enner
 * @since 16 Jun 2023
 */
class Javadoc {

    public static String inherit() {
        // Note: JavaDoc automatically adds the superclass doc,
        // so we don't need to manually call it out.
//        return "{@inheritDoc}";
        return "";
    }

    public static CodeBlock forMessage(RequestInfo.MessageInfo info) {
        return forType("type", info);
    }

    public static CodeBlock.Builder forMessageField(RequestInfo.FieldInfo info) {
        return withComments(info.getSourceLocation())
                .add("<code>$L</code>", getFieldDefinitionLine(info.getDescriptor()));
    }

    public static CodeBlock forEnum(RequestInfo.EnumInfo info) {
        return forType("enum", info);
    }

    public static CodeBlock forEnumValue(RequestInfo.EnumValueInfo info) {
        return withComments(info.getSourceLocation())
                .add("<code>$L = $L;</code>", info.getName(), info.getNumber())
                .build();
    }

    public static CodeBlock.Builder withComments(SourceCodeInfo.Location location) {
        // Protobuf-java seems to prefer leading comments and only use trailing as a fallback
        CodeBlock.Builder builder = CodeBlock.builder();
        String format = "<pre>\n$L</pre>\n\n";
        if (location.hasLeadingComments()) {
            builder.add(format, escapeCommentClose(location.getLeadingComments()));
        } else if (location.hasTrailingComments()) {
            builder.add(format, escapeCommentClose(location.getTrailingComments()));
        }
        return builder;
    }

    private static CodeBlock forType(String name, RequestInfo.TypeInfo info) {
        return withComments(info.getSourceLocation())
                .add("Protobuf $L {@code $T}", name, info.getTypeName())
                .build();
    }

    private static String getFieldDefinitionLine(DescriptorProtos.FieldDescriptorProto descriptor) {
        // optional int32 my_field = 2 [default = 1];
        final String label = descriptor.getLabel().toString()
                .substring("LABEL_".length())
                .toLowerCase(Locale.US);
        String type = descriptor.getTypeName();
        if (type.isEmpty()) {
            type = descriptor.getType().toString()
                    .substring("TYPE_".length())
                    .toLowerCase(Locale.US);
        }
        String definition = String.format("%s %s %s = %d", label, type, descriptor.getName(), descriptor.getNumber());
        String options = "";
        if (descriptor.hasDefaultValue()) {
            String defaultValue = escapeCommentClose(descriptor.getDefaultValue());
            options = " [default = " + defaultValue + "]";
        } else if (descriptor.getOptions().hasPacked()) {
            options = " [packed = " + descriptor.getOptions().getPacked() + "]";
        }

        String line = definition + options + ";";
        return !descriptor.hasExtendee() ? line : "extend {\n  " + line + "\n}";
    }

    private static String escapeCommentClose(String string) {
        if (string.contains("*/")) {
            string = string.replaceAll("\\*/", "*\\\\/");
        }
        return string;
    }

    private Javadoc() {
    }

}
