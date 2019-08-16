package us.hebi.robobuf.benchmarks;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Florian Enner
 * @since 16 Aug 2019
 */
public class PrintMemoryLayout {

    public static void main(String[] args) {
        Class<?> clazz = us.hebi.robobuf.robo.TestAllTypes.class;
        ClassLayout layout = ClassLayout.parseClass(clazz);
        FieldOffsetMap offsetMap = new FieldOffsetMap(layout);

        System.out.println("----  Full Summary ---- ");
        System.out.println(layout.toPrintable());

        System.out.println("---- Order of declared fields ---- ");
        System.out.println("[index => offset (cacheLine)] name");
        List<Field> fields = getInstanceFields(clazz);
        int i = 0;
        for (Field field : fields) {
            long offset = offsetMap.getOffset(field);
            long cacheLine = offset / CACHE_LINE_SIZE; // assumes alignment with cache line boundary, which may not be true
            System.out.println(String.format("[%3d => %3d (%1d)] %s", (i++), offset, cacheLine, field.getName()));
        }

    }

    private static List<Field> getInstanceFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        if (clazz.getSuperclass() != null) {
            fields.addAll(getInstanceFields(clazz.getSuperclass()));
        }
        Stream.of(clazz.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .forEach(fields::add);
        return fields;
    }

    private static class FieldOffsetMap {

        FieldOffsetMap(ClassLayout layout) {
            this.layout = layout;
            for (FieldLayout field : layout.fields()) {
                map.put(field.name(), field);
            }
        }

        public long getOffset(Field field) {
            return map.get(field.getName()).offset();
        }

        final ClassLayout layout;
        final HashMap<String, FieldLayout> map = new HashMap<>();

    }

    private static final int CACHE_LINE_SIZE = 64;

}
