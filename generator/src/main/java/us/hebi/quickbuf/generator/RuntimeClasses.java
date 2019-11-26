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

/**
 * TypeNames of all API classes that can be referenced from generated code
 *
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class RuntimeClasses {

    private static final String API_PACKAGE = "us.hebi.quickbuf";

    static final ClassName ProtoSource = ClassName.get(API_PACKAGE, "ProtoSource");
    static final ClassName ProtoSink = ClassName.get(API_PACKAGE, "ProtoSink");
    static final ClassName ProtoUtil = ClassName.get(API_PACKAGE, "ProtoUtil");
    static final ClassName AbstractMessage = ClassName.get(API_PACKAGE, "ProtoMessage");
    static final ClassName MessageFactory = ClassName.get(API_PACKAGE, "MessageFactory");
    static final ClassName StringType = ClassName.get(API_PACKAGE,"Utf8String");
    static final ClassName BytesType = ClassName.get(API_PACKAGE, "RepeatedByte");
    public static ClassName InvalidProtocolBufferException = ClassName.get(API_PACKAGE, "InvalidProtocolBufferException");
    public static ClassName JsonSink = ClassName.get(API_PACKAGE, "JsonSink");
    public static ClassName ProtoEnum = ClassName.get(API_PACKAGE, "ProtoEnum");
    public static ClassName EnumConverter = ProtoEnum.nestedClass("EnumConverter");

    static final String unknownBytesField = "unknownBytes";
    static final String unknownBytesKey = "unknownBytesJsonKey";

    private static final ClassName RepeatedDouble = ClassName.get(API_PACKAGE, "RepeatedDouble");
    private static final ClassName RepeatedFloat = ClassName.get(API_PACKAGE, "RepeatedFloat");
    private static final ClassName RepeatedLong = ClassName.get(API_PACKAGE, "RepeatedLong");
    private static final ClassName RepeatedInt = ClassName.get(API_PACKAGE, "RepeatedInt");
    private static final ClassName RepeatedBoolean = ClassName.get(API_PACKAGE, "RepeatedBoolean");
    private static final ClassName RepeatedString = ClassName.get(API_PACKAGE, "RepeatedString");
    private static final ClassName RepeatedBytes = ClassName.get(API_PACKAGE, "RepeatedBytes");
    static final ClassName RepeatedMessage = ClassName.get(API_PACKAGE, "RepeatedMessage");
    static final ClassName RepeatedEnum = ClassName.get(API_PACKAGE, "RepeatedEnum");

    static ClassName getRepeatedStoreType(FieldDescriptorProto.Type type) {
        switch (type) {

            case TYPE_DOUBLE:
                return RepeatedDouble;

            case TYPE_FLOAT:
                return RepeatedFloat;

            case TYPE_SFIXED64:
            case TYPE_FIXED64:
            case TYPE_SINT64:
            case TYPE_INT64:
            case TYPE_UINT64:
                return RepeatedLong;

            case TYPE_SFIXED32:
            case TYPE_FIXED32:
            case TYPE_SINT32:
            case TYPE_INT32:
            case TYPE_UINT32:
                return RepeatedInt;

            case TYPE_BOOL:
                return RepeatedBoolean;

            case TYPE_ENUM:
                return RepeatedEnum;

            case TYPE_STRING:
                return RepeatedString;

            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return RepeatedMessage;

            case TYPE_BYTES:
                return RepeatedBytes;

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

}
