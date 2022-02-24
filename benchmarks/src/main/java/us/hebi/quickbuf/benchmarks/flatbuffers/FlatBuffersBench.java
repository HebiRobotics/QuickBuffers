/*-
 * #%L
 * quickbuf-benchmarks
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

package us.hebi.quickbuf.benchmarks.flatbuffers;

import protos.benchmarks.flatbuffers.fb.*;
import com.google.flatbuffers.FlatBufferBuilder;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import protos.benchmarks.flatbuffers.fb.Enum;

import java.nio.ByteBuffer;

/**
 * @author Florian Enner
 * @since 27 Jan 2015
 */
@State(Scope.Thread)
public class FlatBuffersBench {

    public int encode(ByteBuffer buffer) {
        FlatBufferBuilder builder = new FlatBufferBuilder(buffer);

        for (int i = 0; i < vecLen; i++) {

            int name = builder.createString("Hello, World!");

            FooBar.startFooBar(builder);
            FooBar.addName(builder, name);
            FooBar.addRating(builder, 3.1415432432445543543 + i);
            FooBar.addPostfix(builder, (byte) ('!' + i));

            int bar = Bar.createBar(builder,
                    // Foo fields (nested struct)
                    0xABADCAFEABADCAFEL + i,
                    (short) (10000 + i),
                    (byte) ('@' + i),
                    1000000 + i,
                    // Bar fields
                    123456 + i,
                    3.14159f + i,
                    (short) (10000 + i));

            FooBar.addSibling(builder, bar);
            int fooBar = FooBar.endFooBar(builder);
            fooBars[i] = fooBar;

        }

        int list = FooBarContainer.createListVector(builder, fooBars);
        int loc = builder.createString(location);

        FooBarContainer.startFooBarContainer(builder);
        FooBarContainer.addLocation(builder, loc);
        FooBarContainer.addInitialized(builder, initialized);
        FooBarContainer.addFruit(builder, anEnum);
        FooBarContainer.addLocation(builder, loc);
        FooBarContainer.addList(builder, list);
        int fooBarContainer = FooBarContainer.endFooBarContainer(builder);
        builder.finish(fooBarContainer);

        return buffer.position();
    }

    public FooBarContainer decode(ByteBuffer buffer) {
        return FooBarContainer.getRootAsFooBarContainer(buffer);
    }

    public long traverse(FooBarContainer fooBarContainer) {

        long sum = 0;
        sum += fooBarContainer.initialized() ? 1 : 0;
        sum += fooBarContainer.location().length();
        sum += fooBarContainer.fruit();

        int length = fooBarContainer.listLength();
        for (int i = 0; i < length; i++) {

            fooBarContainer.list(fooBar, i);
            sum += fooBar.name().length();
            sum += fooBar.postfix();
            sum += (long) fooBar.rating();

            fooBar.sibling(bar);
            sum += bar.size();
            sum += bar.time();
            sum += bar.ratio();

            bar.parent(foo);
            sum += foo.count();
            sum += foo.id();
            sum += foo.length();
            sum += foo.prefix();

        }

        return sum;

    }

    // reusable read objects
    FooBar fooBar = new FooBar();
    Bar bar = new Bar();
    Foo foo = new Foo();

    private static int vecLen = 3;
    private static int[] fooBars = new int[vecLen];
    private static String location = "http://google.com/flatbuffers/";
    private static boolean initialized = true;
    private static short anEnum = Enum.Bananas;

}
