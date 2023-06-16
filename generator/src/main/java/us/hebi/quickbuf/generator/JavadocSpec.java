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

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.squareup.javapoet.CodeBlock;

/**
 * @author Florian Enner
 * @since 16 Jun 2023
 */
public class JavadocSpec {

    public static String inherit() {
        // Note: JavaDoc automatically adds the superclass doc,
        // so we don't need to manually call it out.
//        return "{@inheritDoc}";
        return "";
    }

    public static CodeBlock forMessage(RequestInfo.MessageInfo info) {
        return forType("type", info);
    }

    public static CodeBlock forEnum(RequestInfo.EnumInfo info) {
        return forType("enum", info);
    }

    private static CodeBlock forType(String name, RequestInfo.TypeInfo info) {
        return withComments(info.getSourceLocation())
                .add("Protobuf $L {@code $T}", name, info.getTypeName())
                .build();
    }

    public static CodeBlock forEnumValue(RequestInfo.EnumValueInfo info) {
        return withComments(info.getSourceLocation())
                .add("<code>$L = $L;</code>", info.getName(), info.getNumber())
                .build();
    }

    public static CodeBlock.Builder withComments(SourceCodeInfo.Location location) {
        CodeBlock.Builder builder = CodeBlock.builder();
        // Protobuf-java seems to prefer leading comments and only use trailing as a fallback
        String format = "<pre>\n$L</pre>\n\n";
        if (location.hasLeadingComments()) {
            builder.add(format, location.getLeadingComments());
        } else if (location.hasTrailingComments()) {
            builder.add(format, location.getTrailingComments());
        }
        return builder;
    }

    public static JavadocSpec builder() {
        return new JavadocSpec();
    }

    public JavadocSpec returns(String format, Object... args) {
        this.returnFormat = format;
        this.returnArgs = args;
        return this;
    }

    public CodeBlock build() {
        CodeBlock.Builder block = CodeBlock.builder();
        if (returnFormat != null) {
            block.add("@return " + returnFormat, returnArgs);
        }
        return block.build();
    }

    private String returnFormat;
    private Object[] returnArgs;

}
