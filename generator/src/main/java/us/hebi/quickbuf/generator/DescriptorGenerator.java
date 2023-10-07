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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

/**
 * @author Florian Enner
 * @since 21 Sep 2023
 */
class DescriptorGenerator {

    static String getDescriptorBytesFieldName() {
        return "descriptorData";
    }

    static String getFileDescriptorFieldName() {
        return "descriptor";
    }

    static String getDescriptorFieldName(RequestInfo.MessageInfo info) {
        // uniquely identifiable descriptor name. Similar to protobuf-java
        // but without the "internal_static_" prefix.
        return info.getFullName().replaceAll("\\.", "_") + "_descriptor";
    }

    public void generate(TypeSpec.Builder type) {

        // bytes shared by everything
        type.addField(FieldSpec.builder(RuntimeClasses.BytesType, getDescriptorBytesFieldName())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(generateEmbeddedByteBlock(fileDescriptorBytes))
                .build());

        // field for the main file descriptor
        CodeBlock.Builder initBlock = CodeBlock.builder();
        initBlock.add("$T.internalBuildGeneratedFileFrom($S, $S, $N", RuntimeClasses.FileDescriptor,
                info.getDescriptor().getName(),
                info.getDescriptor().getPackage(),
                getDescriptorBytesFieldName());

        // any file dependencies
        if (info.getDescriptor().getDependencyCount() > 0) {
            for (String fileName : info.getDescriptor().getDependencyList()) {
                initBlock.add(", $T.getDescriptor()", info.getParentRequest().getInfoForFile(fileName).getOuterClassName());
            }
        }
        initBlock.add(")");

        FieldSpec fileDescriptor = FieldSpec.builder(RuntimeClasses.FileDescriptor, getFileDescriptorFieldName())
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .initializer(initBlock.build())
                .build();
        type.addField(fileDescriptor);

        // Add a static method
        type.addMethod(MethodSpec.methodBuilder("getDescriptor")
                .addJavadoc("@return this proto file's descriptor.")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(RuntimeClasses.FileDescriptor)
                .addStatement("return $N", getFileDescriptorFieldName())
                .build());

        // Descriptor field for each nested type. The message protos should
        // be serialized sequentially, so we start the next search offset where
        // the last descriptor ended.
        int offset = 0;
        for (RequestInfo.MessageInfo message : info.getMessageTypes()) {
            offset = addMessageDescriptor(type, message, offset);
        }

    }

    private int addMessageDescriptor(TypeSpec.Builder type, RequestInfo.MessageInfo message, int startOffset) {
        DescriptorProtos.DescriptorProto msgDesc = message.getDescriptor();
        byte[] descriptorBytes = message.getDescriptor().toByteArray();

        final int offset = findOffset(fileDescriptorBytes, descriptorBytes, startOffset);
        final int length = descriptorBytes.length;

        type.addField(FieldSpec.builder(RuntimeClasses.MessageDescriptor, getDescriptorFieldName(message))
                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                .initializer("$N.internalContainedType($L, $L, $S, $S)",
                        getFileDescriptorFieldName(),
                        offset,
                        length,
                        msgDesc.getName(),
                        message.getFullName())
                .build());

        // Recursively add nested messages
        for (RequestInfo.MessageInfo nestedType : message.getNestedTypes()) {
            startOffset = addMessageDescriptor(type, nestedType, startOffset);
        }

        // Messages should be serialized sequentially, so we start
        // the next search where the current descriptor ends.
        return offset + length;
    }

    private static int findOffset(byte[] data, byte[] part, int start) {
        // Start search at the start guess
        for (int i = start; i < data.length; i++) {
            if (isAtOffset(data, part, i)) {
                return i;
            }
        }
        // Search before just in case
        for (int i = 0; i < start; i++) {
            if (isAtOffset(data, part, i)) {
                return i;
            }
        }
        throw new IllegalArgumentException("part is not contained inside data");
    }

    private static boolean isAtOffset(byte[] data, byte[] part, int offset) {
        for (int i = 0; i < part.length; i++) {
            if (data[offset + i] != part[i]) {
                return false;
            }
        }
        return true;
    }


    private CodeBlock generateEmbeddedByteBlock(byte[] descriptor) {
        // Inspired by Protoc's SharedCodeGenerator::GenerateDescriptors:
        //
        // Embed the descriptor.  We simply serialize the entire FileDescriptorProto
        // and embed it as a string literal, which is parsed and built into real
        // descriptors at initialization time.  We unfortunately have to put it in
        // a string literal, not a byte array, because apparently using a literal
        // byte array causes the Java compiler to generate *instructions* to
        // initialize each and every byte of the array, e.g. as if you typed:
        //   b[0] = 123; b[1] = 456; b[2] = 789;
        // This makes huge bytecode files and can easily hit the compiler's internal
        // code size limits (error "code to large").  String literals are apparently
        // embedded raw, which is what we want.

        // Note: Protobuf uses escaped ISO_8859_1 strings, but for now we use Base64

        // Every block of bytes, start a new string literal, in order to avoid the
        // 64k length limit. Note that this value needs to be <64k.
        final int charsPerLine = 80; // should be a multiple of 4
        final int linesPerPart = 20;
        final int charsPerPart = linesPerPart * charsPerLine;
        final int bytesPerPart = charsPerPart * 3 / 4; // 3x 8 bit => 4x 6 bit

        // Construct bytes from individual base64 String sections
        CodeBlock.Builder initBlock = CodeBlock.builder();
        initBlock.addNamed("$protoUtil:T.decodeBase64(", m).add("$L$>", descriptor.length);

        for (int partIx = 0; partIx < descriptor.length; partIx += bytesPerPart) {
            byte[] part = Arrays.copyOfRange(descriptor, partIx, Math.min(descriptor.length, partIx + bytesPerPart));
            String block = Base64.getEncoder().encodeToString(part);

            String line = block.substring(0, Math.min(charsPerLine, block.length()));
            initBlock.add(",\n$S", line);
            for (int blockIx = line.length(); blockIx < block.length(); blockIx += charsPerLine) {
                line = block.substring(blockIx, Math.min(blockIx + charsPerLine, block.length()));
                initBlock.add(" + \n$S", line);
            }

        }
        initBlock.add(")$<");

        return initBlock.build();
    }

    /**
     * The Protobuf-Java descriptor does some symbol stripping (e.g. jsonName only appears if it was specified),
     * so serializing the raw descriptor does not produce binary compatibility. I don't know whether it's worth
     * implementing it, so for now we leave it empty. See
     * https://github.com/protocolbuffers/protobuf/blob/209accaf6fb91aa26e6086e73626e1884ddfb737/src/google/protobuf/compiler/retention.cc#L105-L116
     * Note that this would also need to be stripped from Message descriptors to work with offsets.
     */
    private static DescriptorProtos.FileDescriptorProto stripSerializedDescriptor(DescriptorProtos.FileDescriptorProto descriptor) {
        return descriptor;
    }

    DescriptorGenerator(RequestInfo.FileInfo info) {
        m.put("abstractMessage", RuntimeClasses.AbstractMessage);
        m.put("protoUtil", RuntimeClasses.ProtoUtil);
        this.info = info;
        this.fileDescriptorBytes = stripSerializedDescriptor(info.getDescriptor()).toByteArray();
    }

    final RequestInfo.FileInfo info;
    final HashMap<String, Object> m = new HashMap<>();
    final byte[] fileDescriptorBytes;

}
