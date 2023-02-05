/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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
package us.hebi.quickbuf.benchmarks.jdk;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.quickbuf.JdkMath;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * === Zulu 8 ===
 * Benchmark                                      Mode  Cnt    Score    Error   Units
 * JdkMethodsBenchmark.testMultiplyHigh_wrapper  thrpt   20  572,132 ± 45,258  ops/ms
 * <p>
 * === Zulu 17 ===
 * Benchmark                                      Mode  Cnt     Score    Error   Units
 * JdkMethodsBenchmark.testMultiplyHigh_direct   thrpt   20  1848,684 ± 64,099  ops/ms
 * JdkMethodsBenchmark.testMultiplyHigh_wrapper  thrpt   20  1853,370 ± 46,721  ops/ms
 * <p>
 * === Graal 17 ===
 * Benchmark                                      Mode  Cnt    Score    Error   Units
 * JdkMethodsBenchmark.testMultiplyHigh_direct   thrpt   20  417,786 ± 33,152  ops/ms
 * JdkMethodsBenchmark.testMultiplyHigh_wrapper  thrpt   20  436,781 ± 22,462  ops/ms
 *
 * @author Florian Enner
 * @since 22 Jan 2023
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JdkMethodsBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JdkMethodsBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final long[] lValues = new long[1024];

    {
        final Random rnd = new Random(0);
        for (int i = 0; i < lValues.length; i++) {
            lValues[i] = rnd.nextLong();
        }
    }

    @Benchmark
    public long testMultiplyHigh_wrapper() {
        long result = 0;
        for (int i = 1; i < lValues.length; i++) {
            result |= JdkMath.multiplyHigh(lValues[i - 1], lValues[i]);
        }
        return result;
    }

/*    @Benchmark
    public long testMultiplyHigh_direct() {
        long result = 0;
        for (int i = 1; i < lValues.length; i++) {
            result |= java.lang.Math.multiplyHigh(lValues[i - 1], lValues[i]);
        }
        return result;
    }*/

}
