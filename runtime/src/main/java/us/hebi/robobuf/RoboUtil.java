package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public interface RoboUtil {

    public static boolean equals(CharSequence a, CharSequence b) {
        if (a.length() != b.length())
            return false;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                return false;
        }

        return true;
    }

    public static boolean equals(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    public static boolean equals(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    public static boolean equals(int a, int b) {
        return a == b;
    }

    public static boolean equals(long a, long b) {
        return a == b;
    }

    public static boolean equals(boolean a, boolean b) {
        return a == b;
    }

    static final boolean _booleanDefault = false;
    static final float _floatDefault = 0f;
    static final double _doubleDefault = 0d;
    static final int _intDefault = 0;
    static final long _longDefault = 0L;
    static final byte _byteDefault = (byte) 0;

}
