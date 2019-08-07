package us.hebi.robobuf.compiler;

import com.google.common.base.CaseFormat;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public class NameUtil {

    public static String toUpperCamel(String name) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
    }

    public static String toLowerCamel(String name) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
    }

}
