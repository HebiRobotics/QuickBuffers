package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class Platform {

    static int getJavaVersion() {
        return javaVersion;
    }

    static {
        String specVersion = System.getProperty("java.specification.version", "1.6");
        String[] parts = specVersion.split("\\.");

        // 1.4, 1.5, 1.6, 1.8, 9, 10, 11, 18.3, etc.
        if ("1".equals(parts[0])) {
            javaVersion = Integer.parseInt(parts[1]);
        } else {
            javaVersion = Integer.parseInt(parts[0]);
        }
    }

    private static final int javaVersion;

}
