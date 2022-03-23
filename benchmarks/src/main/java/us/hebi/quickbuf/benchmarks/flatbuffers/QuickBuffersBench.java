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

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import protos.benchmarks.flatbuffers.quickbuf.*;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;

/**
 * @author Florian Enner
 * @since 28 Oct 2019
 */
@State(Scope.Thread)
public class QuickBuffersBench {

    static FooBarContainer setData(FooBarContainer fooBarContainer) {
        fooBarContainer.clearQuick()
                .setFruitValue(anEnum)
                .setInitialized(initialized)
                .setLocation(location);

        for (int i = 0; i < vecLen; i++) {

            fooBarContainer.getMutableList().next()
                    .setRating(3.1415432432445543543 + i)
                    .setPostfix('!' + i)
                    .setName("Hello, World!")

                    .getMutableSibling()
                    .setRatio(3.14159f + i)
                    .setTime(123456 + i)
                    .setSize(10000 + i)

                    .getMutableParent()
                    .setId(0xABADCAFEABADCAFEL + i)
                    .setCount(10000 + i)
                    .setPrefix('@' + i)
                    .setLength(1000000 + i);

        }

        return fooBarContainer;
    }

    public int encode() {
        try {
            sink.setOutput(encodeBuffer);
            setData(encodeMsg).writeTo(sink);
            return sink.getTotalBytesWritten();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public FooBarContainer decode() {
        try {
            return decodeMsg.clearQuick().mergeFrom(source.setInput(decodeBuffer));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public long traverse(FooBarContainer fooBarContainer) {
        long sum = 0;
        sum += fooBarContainer.getInitialized() ? 1 : 0;
        sum += fooBarContainer.getLocation().length();
        sum += fooBarContainer.getFruit().getNumber();

        for (FooBar fooBar : fooBarContainer.getList()) {

            sum += fooBar.getPostfix();
            sum += (long) fooBar.getRating();
            sum += fooBar.getNameBytes().size();

            Bar bar = fooBar.getSibling();
            sum += bar.getRatio();
            sum += bar.getTime();
            sum += bar.getSize();

            Foo foo = bar.getParent();
            sum += foo.getId();
            sum += foo.getCount();
            sum += foo.getPrefix();
            sum += foo.getLength();

        }

        return sum;

    }

    final FooBarContainer encodeMsg = setData(FooBarContainer.newInstance());

    byte[] encodeBuffer = encodeMsg.toByteArray();
    ProtoSink sink = ProtoSink.newDirectSink().setOutput(encodeBuffer);

    FooBarContainer decodeMsg = FooBarContainer.newInstance();
    byte[] decodeBuffer = encodeBuffer.clone();
    ProtoSource source = ProtoSource.newDirectSource().setInput(decodeBuffer);

    private static int vecLen = 3;
    private static String location = "http://google.com/flatbuffers/";
    private static boolean initialized = true;
    private static int anEnum = Fruit.Bananas_VALUE;

}
