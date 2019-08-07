package us.hebi.robobuf.compiler.field;

import static com.google.common.base.CaseFormat.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 19 Jun 2015
 */
public class FieldUtil {

    public static String memberName(String field) {
        return LOWER_UNDERSCORE.to(LOWER_CAMEL, field) + "_";
    }

    public static String getterName(String field) {
        return "get" + LOWER_UNDERSCORE.to(UPPER_CAMEL, field);
    }

    public static String mutableGetterName(String field) {
        return "getMutable" + LOWER_UNDERSCORE.to(UPPER_CAMEL, field);
    }

    public static String setterName(String field) {
        return "set" + LOWER_UNDERSCORE.to(UPPER_CAMEL, field);
    }

    public static String hazzerName(String field) {
        return "has" + LOWER_UNDERSCORE.to(UPPER_CAMEL, field);
    }

    public static String clearerName(String field) {
        return "clear" + LOWER_UNDERSCORE.to(UPPER_CAMEL, field);
    }

    static final int WIRETYPE_VARINT = 0;
    static final int WIRETYPE_FIXED64 = 1;
    static final int WIRETYPE_LENGTH_DELIMITED = 2;
    static final int WIRETYPE_START_GROUP = 3;
    static final int WIRETYPE_END_GROUP = 4;
    static final int WIRETYPE_FIXED32 = 5;
    static final int TAG_TYPE_BITS = 3;
    static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

}
