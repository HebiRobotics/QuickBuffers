package us.hebi.robobuf.benchmarks.comparison;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import uk.co.real_logic.robo.fix.Fix;
import us.hebi.robobuf.ProtoSink;
import us.hebi.robobuf.ProtoSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Throughput test. Comparable to
 * https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html
 *
 * === Robobuf (with Unsafe)
 *  Benchmark                           Mode  Cnt     Score     Error   Units
 *  SbeMarketBenchmarkRobo.testDecode  thrpt   10  5814.417 ± 322.773  ops/ms
 *  SbeMarketBenchmarkRobo.testEncode  thrpt   10  7443.789 ±  82.819  ops/ms
 *
 * @author Florian Enner
 * @since 16 Oct 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeMarketBenchmarkRobo {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeMarketBenchmarkRobo.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Fix.MarketDataIncrementalRefreshTrades marketData = new Fix.MarketDataIncrementalRefreshTrades();
    final byte[] decodeBuffer = fillMarketData(marketData).toByteArray();
    final byte[] encodeBuffer = new byte[decodeBuffer.length];

    final ProtoSource source = ProtoSource.createFastest();
    final ProtoSink sink = ProtoSink.createFastest();

    @Benchmark
    public int testEncode() throws IOException {
        sink.setOutput(encodeBuffer);
        fillMarketData(marketData).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public Object testDecode() throws IOException {
        source.setInput(decodeBuffer);
        return marketData.clearQuick().mergeFrom(source);
    }

    private static Fix.MarketDataIncrementalRefreshTrades fillMarketData(Fix.MarketDataIncrementalRefreshTrades marketData) {
        marketData.clearQuick()
                .setTransactTime(1234L)
                .setEventTimeDelta(987)
                .setMatchEventIndicator(Fix.MarketDataIncrementalRefreshTrades.MatchEventIndicator.END_EVENT);

        for (int i = 0; i < 2; i++) {

            Fix.MdIncGrp group = marketData.getMutableMdIncGroup().next()
                    .setTradeId(1234L)
                    .setSecurityId(56789L)
                    .setNumberOfOrders(1)
                    .setRepSeq(1)
                    .setMdUpdateAction(Fix.MdIncGrp.MdUpdateAction.NEW)
                    .setAggressorSide(Fix.MdIncGrp.Side.BUY)
                    .setMdEntryType(Fix.MdIncGrp.MdEntryType.BID);
            group.getMutableMdEntryPx().setMantissa(50);
            group.getMutableMdEntrySize().setMantissa(10);

        }

        return marketData;
    }


}
