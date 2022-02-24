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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import lombok.ToString;
import us.hebi.quickbuf.generator.RequestInfo.FileInfo;
import us.hebi.quickbuf.generator.RequestInfo.MessageInfo;
import us.hebi.quickbuf.generator.RequestInfo.TypeInfo;

import java.util.HashMap;
import java.util.Map;

import static us.hebi.quickbuf.generator.Preconditions.*;

/**
 * Maps proto type ids to Java class names, e.g.,
 *
 * .proto.package.Message.SubMessage -> us.hebi.quickbuf.OuterMessage.Message.SubMessage
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
@ToString
class TypeRegistry {

    static TypeRegistry empty() {
        return new TypeRegistry();
    }

    TypeName resolveJavaTypeFromProto(FieldDescriptorProto descriptor) {
        switch (descriptor.getType()) {

            case TYPE_DOUBLE:
                return TypeName.DOUBLE;
            case TYPE_FLOAT:
                return TypeName.FLOAT;
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return TypeName.LONG;
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_SINT32:
            case TYPE_INT32:
            case TYPE_UINT32:
                return TypeName.INT;
            case TYPE_BOOL:
                return TypeName.BOOLEAN;
            case TYPE_STRING:
                return TypeName.get(String.class);
            case TYPE_ENUM:
            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return resolveMessageType(descriptor.getTypeName());
            case TYPE_BYTES:
                return RuntimeClasses.BytesType;

        }
        throw new GeneratorException("Unsupported type: " + descriptor);
    }

    // package-private for unit tests
    TypeName resolveMessageType(String typeId) {
        return checkNotNull(typeMap.get(typeId), "Unable to resolve type id: " + typeId);
    }

    void registerContainedTypes(RequestInfo info) {
        typeMap.clear();
        for (FileInfo file : info.getFiles()) {
            file.getMessageTypes().forEach(this::registerType);
            file.getEnumTypes().forEach(this::registerType);
        }
    }

    private void registerType(TypeInfo typeInfo) {
        if (typeMap.containsValue(typeInfo.getTypeName()))
            throw new GeneratorException("Duplicate class name: " + typeInfo.getTypeName());
        if (typeMap.put(typeInfo.getTypeId(), typeInfo.getTypeName()) != null)
            throw new GeneratorException("Duplicate type id: " + typeInfo.getTypeId());

        if (typeInfo instanceof MessageInfo) {
            ((MessageInfo) typeInfo).getNestedTypes().forEach(this::registerType);
            ((MessageInfo) typeInfo).getNestedEnums().forEach(this::registerType);
        }
    }

    private TypeRegistry() {
    }

    final Map<String, ClassName> typeMap = new HashMap<>();

}
