package us.hebi.robobuf.compiler;

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
class NameUtil {

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
     * @param input
     * @param cap_next_letter
     * @return
     */
    private static String underscoresToCamelCaseImpl(CharSequence input, boolean cap_next_letter) {
        StringBuilder result = new StringBuilder(input.length());
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
            "false", "null", "true"
    ));

}
