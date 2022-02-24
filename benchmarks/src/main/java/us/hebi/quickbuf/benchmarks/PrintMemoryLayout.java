/*-
 * #%L
 * benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
