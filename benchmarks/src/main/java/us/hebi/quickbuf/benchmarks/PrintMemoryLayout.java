/*-
 * #%L
 * benchmarks
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

package us.hebi.quickbuf.benchmarks;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;
import us.hebi.quickbuf.ProtoMessage;
import protos.test.quickbuf.TestAllTypes;

import java.io.IOException;
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

    public static void main(String[] args) throws IllegalAccessException, IOException {
        Class<?> clazz = protos.test.quickbuf.TestAllTypes.class;
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
            String string = String.format("[%3d => %3d (%1d)] %s", (i++), offset, cacheLine, field.getName());
            System.out.println(string);
        }

        System.out.println("---- Object Graph ---- ");
        ProtoMessage msg = TestAllTypes.newInstance();
        System.out.println(GraphLayout.parseInstance(msg).toPrintable());

        System.out.println("---- Offsets of Nested Messages ---- ");
        printNestedOffsets(msg);

    }

    private static void printNestedOffsets(ProtoMessage message) throws IllegalAccessException {
        printNestedOffsets(null, message.getClass().getSimpleName(), message, 0);
    }

    /**
     * running it causes allocations and mess with results. unreliable
     */
    private static void printNestedOffsets(ProtoMessage parent, String name, ProtoMessage message, int indents) throws IllegalAccessException {
        for (int i = 0; i < indents; i++) {
            System.out.print(" ");
        }

        long endOfParent = parent == null ? vm.addressOf(message) : vm.addressOf(parent) + vm.sizeOf(parent);
        long start = vm.addressOf(message) - endOfParent;
        long end = start + vm.sizeOf(message);
        System.out.println(String.format("%d-%d : %s", start, end, name));

        for (Field field : message.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            Object value = field.get(message);
            if (value instanceof ProtoMessage) {
                printNestedOffsets(message, field.getName(), (ProtoMessage) value, indents + 2);
            }

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
    private static VirtualMachine vm = VM.current();

}
