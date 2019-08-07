package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import lombok.Value;

import java.util.Map;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
@Value
public class FileParams {

    public FileParams(Map<String, String> genParams, FileDescriptorProto file) {

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

}
