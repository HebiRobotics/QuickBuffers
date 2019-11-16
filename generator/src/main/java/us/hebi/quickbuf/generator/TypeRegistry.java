/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
                return RuntimeClasses.StringType;
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
