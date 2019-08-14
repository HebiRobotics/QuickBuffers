package us.hebi.robobuf.compiler.field;

import us.hebi.robobuf.compiler.GeneratorException;
import us.hebi.robobuf.compiler.RequestInfo.FieldInfo;
import us.hebi.robobuf.compiler.field.BytesField.OptionalBytesField;
import us.hebi.robobuf.compiler.field.BytesField.RepeatedBytesField;
import us.hebi.robobuf.compiler.field.EnumField.OptionalEnumField;
import us.hebi.robobuf.compiler.field.EnumField.RepeatedEnumField;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class FieldGenerators {

    public static FieldGenerator createGenerator(FieldInfo field) {

        switch (field.getDescriptor().getType()) {

            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_INT64:
            case TYPE_UINT64:
            case TYPE_INT32:
            case TYPE_FIXED64:
            case TYPE_FIXED32:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SFIXED64:
            case TYPE_SINT32:
            case TYPE_SINT64:
            case TYPE_BOOL:
                return field.isRepeated() ? new FieldGenerator(field) : new FieldGenerator(field);

            case TYPE_GROUP:
            case TYPE_MESSAGE:
                return field.isRepeated() ? new FieldGenerator(field) : new FieldGenerator(field);

            case TYPE_ENUM:
                return field.isRepeated() ? new RepeatedEnumField(field) : new OptionalEnumField(field);

            case TYPE_STRING:
                return field.isRepeated() ? new FieldGenerator(field) : new FieldGenerator(field);

            case TYPE_BYTES:
                return field.isRepeated() ? new RepeatedBytesField(field) : new OptionalBytesField(field);

        }

        throw new GeneratorException("Unsupported field:\n" + field.getDescriptor());
    }

}
