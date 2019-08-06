package us.hebi.robobuf.compiler;

import com.google.common.base.CaseFormat;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import lombok.Value;

import java.io.File;
import java.util.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
@Value
public class FileParams {

    public FileParams(Map<String, String> generatorParams, FileDescriptorProto file, List<String> errors) {

        fileName = file.getName();
        protoPackage = file.hasPackage() ? file.getPackage() : DEFAULT_PACKAGE;

        // Update parameters for this particular file
        FileOptions options = file.getOptions();
        generateMultipleFiles = options.hasJavaMultipleFiles() && options.getJavaMultipleFiles();

        // Use specified name or default to the name of the file
        if (options.hasJavaOuterClassname()) {
            outerClassname = options.getJavaOuterClassname();
        } else {
            String defaultClassName = toUpperCamel(stripSuffixString(new File(fileName).getName())); // removes slashes etc.
            if (hasConflictingClassName(file, defaultClassName)) {
                defaultClassName += "OuterClass"; // same as in recent versions of proto-java
            }
            outerClassname = defaultClassName;
        }

        // Use specified package or convert proto package
        if (options.hasJavaPackage()) {
            javaPackage = options.getJavaPackage();
        } else {
            javaPackage = protoPackage;
        }

        deprecated = options.hasDeprecated() && options.getDeprecated();

    }

    private static boolean hasConflictingClassName(FileDescriptorProto file, String outerClassname) {
        for (DescriptorProto descriptor : file.getMessageTypeList()) {
            if (outerClassname.equals(toUpperCamel(descriptor.getName())))
                return true;
        }
        for (EnumDescriptorProto descriptor : file.getEnumTypeList()) {
            if (outerClassname.equals(toUpperCamel(descriptor.getName())))
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

    String fileName;
    boolean generateMultipleFiles;

    private String outerClassname;
    private String protoPackage;
    private String javaPackage;

    private boolean deprecated;

    // TODO create a map of all types for validation
    private static final String DEFAULT_PACKAGE = "";
    private final Map<String, String> javaPackages = new HashMap<>();
    private final Map<String, String> javaOuterClassnames = new HashMap<>();
    private final Set<String> multipleFileSet = new HashSet<>();

}
