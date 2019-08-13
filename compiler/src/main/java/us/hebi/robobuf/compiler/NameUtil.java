package us.hebi.robobuf.compiler;

import com.google.common.base.CaseFormat;

import java.util.Arrays;
import java.util.HashSet;

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

    public static String filterKeyword(String name) {
        return isReservedKeyword(name) ? name + "_" : name;
    }

    private static boolean isReservedKeyword(String key) {
        return keywordSet.contains(key);
    }

    private static final HashSet<String> keywordSet = new HashSet<>(Arrays.asList(
            // Reserved Java Keywords
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",

            // Reserved Keywords for Literals
            "false", "null", "true"
    ));

}
