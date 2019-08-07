package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import lombok.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
@Value
public class FileParams {

    public FileParams(Map<String, String> generatorParams, FileDescriptorProto file) {

        fileName = file.getName();
        protoPackage = NameResolver.getProtoPackage(file);
        javaPackage = NameResolver.getJavaPackage(file);
        outerClassname = NameResolver.getJavaOuterClassname(file);

        FileOptions options = file.getOptions();
        generateMultipleFiles = options.hasJavaMultipleFiles() && options.getJavaMultipleFiles();
        deprecated = options.hasDeprecated() && options.getDeprecated();
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
