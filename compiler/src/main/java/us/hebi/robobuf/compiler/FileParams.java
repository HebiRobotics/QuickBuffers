package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import lombok.Getter;

import java.util.*;

import static us.hebi.robobuf.compiler.Preconditions.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
@Getter
public class FileParams {

    public FileParams(Map<String, String> generatorParams, FileDescriptorProto file, List<String> errors) {

        name = file.getName();

        // Update parameters for this particular file
        FileOptions options = file.getOptions();
        outerClassName = options.hasJavaOuterClassname() ? options.getJavaOuterClassname() : "";
        javaPackage = options.hasJavaPackage() ? options.getJavaPackage() : "";
        useMultipleFiles = options.hasJavaMultipleFiles() && options.getJavaMultipleFiles();
        deprecated = options.hasDeprecated() && options.getDeprecated();

        addToDependencyMapRecursive(file);

        // Overwrite any existing options with ones from command line
        // (Uses older javanano parameters)
        generatorParams.forEach((key, value) -> {
            key = key.trim();
            value = value.trim();
            String[] parts;

            switch (key) {

                case "java_package":
                    parts = value.trim().split("|");
                    checkArgument(parts.length == 2, "Bad java_package, expecting filename|PackageName. Found: '" + value + "'");
                    javaPackages.put(parts[0], parts[1]);
                    break;

                case "java_outer_classname":
                    parts = value.trim().split("|");
                    checkArgument(parts.length == 2, "Bad java_outer_classname, expecting filename|ClassName. Found: '" + value + "'");
                    javaOuterClassnames.put(parts[0], parts[1]);
                    break;

                case "java_multiple_files":
                    useMultipleFiles = "true".equals(value);
                    useMultipleFilesWasOverriden = true;
                    break;

                case "store_unknown_fields":
                    storeUnknownFields = "true".equals(value);
                    break;

                default:
                    errors.add("Ignoring unknown generator option: " + key);
                    break;

            }


        });

    }

    private void addToDependencyMapRecursive(FileDescriptorProto file) {
        String fileName = file.getName();
        FileOptions options = file.getOptions();
        if (options.hasJavaPackage())
            javaPackages.put(fileName, options.getJavaPackage());
        if (options.hasJavaMultipleFiles())
            multipleFileSet.add(fileName);
        if (options.hasJavaOuterClassname())
            javaOuterClassnames.put(fileName, options.getJavaOuterClassname());

        for (String dependency : file.getDependencyList()) {
            // TODO: seems that the proto format was changed?
            //addToDependencyMapRecursive(dependency);
        }
    }

    private final String name;
    private String outerClassName;
    private String javaPackage;
    private boolean useMultipleFiles;
    private boolean useMultipleFilesWasOverriden = false;
    private boolean deprecated = false;
    private boolean storeUnknownFields = false;

    private final Map<String, String> javaPackages = new HashMap<>();
    private final Map<String, String> javaOuterClassnames = new HashMap<>();
    private final Set<String> multipleFileSet = new HashSet<>();

}
