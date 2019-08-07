package us.hebi.robobuf.compiler;

import com.google.common.base.CaseFormat;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

import java.io.File;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class NameResolver {

    static String getJavaPackage(FileDescriptorProto descriptor) {
        if (descriptor.getOptions().hasJavaPackage())
            return descriptor.getOptions().getJavaPackage();
        return getProtoPackage(descriptor);
    }

    static String getProtoPackage(FileDescriptorProto descriptor) {
        if (descriptor.hasPackage())
            return descriptor.getPackage();
        return DEFAULT_PACKAGE;
    }

    static String getJavaOuterClassname(FileDescriptorProto descriptor) {
        if (descriptor.getOptions().hasJavaOuterClassname())
            return descriptor.getOptions().getJavaOuterClassname();

        String nameWithoutPath = new File(descriptor.getName()).getName(); // removes slashes etc.
        String defaultOuterClassName = toUpperCamel(stripSuffixString(nameWithoutPath));

        // add suffix on collisions to match gen-java behavior
        if(!hasConflictingClassName(descriptor, defaultOuterClassName))
            return defaultOuterClassName;
        return defaultOuterClassName + OUTER_CLASS_SUFFIX;

    }

    private static boolean hasConflictingClassName(FileDescriptorProto descriptor, String outerClassName) {
        for (DescriptorProto messageDescriptor : descriptor.getMessageTypeList()) {
            if (outerClassName.equals(toUpperCamel(messageDescriptor.getName())))
                return true;
        }
        for (DescriptorProtos.EnumDescriptorProto enumDescriptor : descriptor.getEnumTypeList()) {
            if (outerClassName.equals(toUpperCamel(enumDescriptor.getName())))
                return true;
        }
        return false;
    }

    private static String toUpperCamel(String name) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
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

}
