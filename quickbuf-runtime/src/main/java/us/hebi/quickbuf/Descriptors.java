/*-
 * #%L
 * quickbuf-runtime
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
package us.hebi.quickbuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contains classes that describe the protocol message types.
 *
 * @author Florian Enner
 * @since 20 Sep 2023
 */
public final class Descriptors {

    public abstract static class GenericDescriptor {

        public byte[] toProtoBytes() {
            return Arrays.copyOfRange(bytes.array, offset, offset + length);
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return fullName;
        }

        public abstract FileDescriptor getFile();

        // Private constructor to prevent subclasses outside this package
        private GenericDescriptor(String fullName, String name, RepeatedByte bytes, int offset, int length) {
            this.fullName = fullName;
            this.name = name;
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public String toString() {
            return "descriptor{" + fullName + "}";
        }

        final String fullName;
        final String name;
        final RepeatedByte bytes;
        final int offset;
        final int length;

    }

    public static class FileDescriptor extends GenericDescriptor {

        public static FileDescriptor internalBuildGeneratedFileFrom(String name, String protoPackage, RepeatedByte bytes, FileDescriptor... dependencies) {
            return new FileDescriptor(name, protoPackage, bytes, Arrays.asList(dependencies));
        }

        public Descriptor internalContainedType(int offset, int length, String name, String fullName) {
            Descriptor type = new Descriptor(fullName, name, this, bytes, offset, length);
            containedTypes.add(type);
            return type;
        }

        public String getPackage() {
            return protoPackage;
        }

        public FileDescriptor getFile() {
            return this;
        }

        public List<FileDescriptor> getDependencies() {
            return Collections.unmodifiableList(dependencies);
        }

        /**
         * @return descriptors for all message and nested types contained in this file
         */
        public List<Descriptor> getAllContainedTypes() {
            return Collections.unmodifiableList(containedTypes);
        }

        /**
         * @return descriptors for all types in this file and its dependencies
         */
        public List<Descriptor> getAllKnownTypes() {
            return getAllKnownTypes(this, new ArrayList<Descriptor>());
        }

        private static List<Descriptor> getAllKnownTypes(FileDescriptor file, List<Descriptor> list) {
            for (FileDescriptor dependency : file.dependencies) {
                getAllKnownTypes(dependency, list);
            }
            list.addAll(file.containedTypes);
            return list;
        }

        private FileDescriptor(String fileName, String protoPackage, RepeatedByte bytes, List<FileDescriptor> dependencies) {
            super(fileName, fileName, bytes, 0, bytes.length());
            this.protoPackage = protoPackage;
            this.dependencies = dependencies;
        }

        final String protoPackage;
        final List<FileDescriptor> dependencies;
        final List<Descriptor> containedTypes = new ArrayList<Descriptor>();

    }

    public static class Descriptor extends GenericDescriptor {

        private Descriptor(String fullName, String name, FileDescriptor file, RepeatedByte bytes, int offset, int length) {
            super(fullName, name, bytes, offset, length);
            this.file = file;
        }

        @Override
        public FileDescriptor getFile() {
            return file;
        }

        final FileDescriptor file;

    }

    private Descriptors() {
    }

}
