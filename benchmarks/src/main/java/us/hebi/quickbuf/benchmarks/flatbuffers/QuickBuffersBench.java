/*-
 * #%L
 * quickbuf-benchmarks
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
            sink.wrap(encodeBuffer);
            setData(encodeMsg).writeTo(sink);
            return sink.position();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public FooBarContainer decode() {
        try {
            return decodeMsg.clearQuick().mergeFrom(source.wrap(decodeBuffer));
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
            sum += fooBar.getNameBytes().getChars(chars).length();

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

    final StringBuilder chars = new StringBuilder(32);
    final FooBarContainer encodeMsg = setData(FooBarContainer.newInstance());

    byte[] encodeBuffer = encodeMsg.toByteArray();
    ProtoSink sink = ProtoSink.newInstance().wrap(encodeBuffer);

    FooBarContainer decodeMsg = FooBarContainer.newInstance();
    byte[] decodeBuffer = encodeBuffer.clone();
    ProtoSource source = ProtoSource.newInstance().wrap(decodeBuffer);

    private static int vecLen = 3;
    private static String location = "http://google.com/flatbuffers/";
    private static boolean initialized = true;
    private static int anEnum = Fruit.Bananas_VALUE;

}
