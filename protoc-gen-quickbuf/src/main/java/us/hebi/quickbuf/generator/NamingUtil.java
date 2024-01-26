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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Utilities for dealing with names
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class NamingUtil {

    static String getJavaPackage(DescriptorProtos.FileDescriptorProto descriptor) {
        if (descriptor.getOptions().hasJavaPackage())
            return descriptor.getOptions().getJavaPackage();
        return getProtoPackage(descriptor);
    }

    static String getProtoPackage(DescriptorProtos.FileDescriptorProto descriptor) {
        if (descriptor.hasPackage())
            return descriptor.getPackage();
        return DEFAULT_PACKAGE;
    }

    static String getJavaOuterClassname(DescriptorProtos.FileDescriptorProto descriptor) {
        if (descriptor.getOptions().hasJavaOuterClassname())
            return descriptor.getOptions().getJavaOuterClassname();

        String nameWithoutPath = new File(descriptor.getName()).getName(); // removes slashes etc.
        String defaultOuterClassName = toUpperCamel(stripSuffixString(nameWithoutPath));

        // add suffix on collisions to match gen-java behavior
        if (!hasConflictingClassName(descriptor, defaultOuterClassName))
            return defaultOuterClassName;
        return defaultOuterClassName + OUTER_CLASS_SUFFIX;

    }

    private static boolean hasConflictingClassName(DescriptorProtos.FileDescriptorProto descriptor, String outerClassName) {
        for (DescriptorProtos.DescriptorProto messageDescriptor : descriptor.getMessageTypeList()) {
            if (outerClassName.equals(toUpperCamel(messageDescriptor.getName())))
                return true;
        }
        for (DescriptorProtos.EnumDescriptorProto enumDescriptor : descriptor.getEnumTypeList()) {
            if (outerClassName.equals(toUpperCamel(enumDescriptor.getName())))
                return true;
        }
        return false;
    }

    private static String stripSuffixString(String fileName) {
        if (fileName.endsWith(".proto"))
            return fileName.substring(0, fileName.length() - ".proto".length());
        if (fileName.endsWith(".protodevel"))
            return fileName.substring(0, fileName.length() - ".protodevel".length());
        return fileName;
    }

    private static String DEFAULT_PACKAGE = "";
    private static String OUTER_CLASS_SUFFIX = "OuterClass";

    static String toUpperCamel(String name) {
        return underscoresToCamelCaseImpl(name, true);
    }

    /**
     * Port of JavaNano's "UnderscoresToCamelCaseImpl". Guava's CaseFormat doesn't
     * write upper case after numbers, so the names wouldn't be consistent.
     *
     * @param input original name with lower_underscore case
     * @param capFirstLetter true if the first letter should be capitalized
     * @return camelCase
     */
    private static String underscoresToCamelCaseImpl(CharSequence input, boolean capFirstLetter) {
        StringBuilder result = new StringBuilder(input.length());
        boolean cap_next_letter = capFirstLetter;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if ('a' <= c && c <= 'z') {
                if (cap_next_letter) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(c);
                }
                cap_next_letter = false;
            } else if ('A' <= c && c <= 'Z') {
                if (i == 0 && !cap_next_letter) {
                    // Force first letter to lower-case unless explicitly told to
                    // capitalize it.
                    result.append(Character.toLowerCase(c));
                } else {
                    // Capital letters after the first are left as-is.
                    result.append(c);
                }
                cap_next_letter = false;
            } else if ('0' <= c && c <= '9') {
                result.append(c);
                cap_next_letter = true;
            } else {
                cap_next_letter = true;
            }
        }
        return result.toString();
    }

    public static String filterKeyword(String name) {
        return keywordSet.contains(name) ? name + "_" : name;
    }

    public static boolean isCollidingFieldName(String field) {
        return collidingFieldSet.contains(field);
    }

    private static final HashSet<String> keywordSet = new HashSet<>(Arrays.asList(
            // Reserved Java Keywords
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",

            // Reserved Keywords for Literals
            "false", "null", "true",

            // Reserved names for internal variables
            "value", "values", "input", "output", "tag",
            "other", "o", "size", "unknownBytes",
            "cachedSize", "bitfield0_", "unknownBytesFieldName"
    ));

    private static final HashSet<String> collidingFieldSet = withCamelCaseNames(
            "class", // Object::getClass
            "quick", // clearQuick
            "missing_fields", // getMissingFields
            "unknown_bytes",// getUnknownFields
            "serialized_size", // getSerializedSize
            "cached_size", // getSerializedSize
            "descriptor" // getDescriptor
    );

    private static HashSet<String> withCamelCaseNames(String... fieldNames) {
        HashSet<String> set = new HashSet<>(fieldNames.length * 2);
        for (String fieldName : fieldNames) {
            set.add(fieldName);
            set.add(underscoresToCamelCaseImpl(fieldName, false));
        }
        return set;
    }

}
