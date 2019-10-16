package us.hebi.robobuf.benchmarks.comparison;

import com.google.protobuf.CodedOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import uk.co.real_logic.protobuf.fix.Fix;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Throughput test. Comparable to
 * https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html
 *
 * === Protobuf-Java ===
 * Benchmark                            Mode  Cnt     Score     Error   Units
 * SbeMarketBenchmarkProto.testDecode  thrpt   10  3289.597 ± 136.637  ops/ms
 * SbeMarketBenchmarkProto.testEncode  thrpt   10  3811.349 ±  97.316  ops/ms
 *
 * @author Florian Enner
 * @since 16 Oct 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeMarketBenchmarkProto {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeMarketBenchmarkProto.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Fix.MarketDataIncrementalRefreshTrades.Builder marketData = Fix.MarketDataIncrementalRefreshTrades.newBuilder();
    final byte[] decodeBuffer = fillMarketData(marketData).build().toByteArray();
    final byte[] encodeBuffer = new byte[decodeBuffer.length];

    @Benchmark
    public int testEncode() throws IOException {
        final CodedOutputStream output = CodedOutputStream.newInstance(encodeBuffer);
        Fix.MarketDataIncrementalRefreshTrades trades = fillMarketData(marketData).build();
        trades.writeTo(output);
        return output.getTotalBytesWritten();
    }

    @Benchmark
    public Object testDecode() throws IOException {
        return Fix.MarketDataIncrementalRefreshTrades.parseFrom(decodeBuffer);
    }

    private static Fix.MarketDataIncrementalRefreshTrades.Builder fillMarketData(final Fix.MarketDataIncrementalRefreshTrades.Builder marketData) {
        marketData.clear()
                .setTransactTime(1234L)
                .setEventTimeDelta(987)
                .setMatchEventIndicator(Fix.MarketDataIncrementalRefreshTrades.MatchEventIndicator.END_EVENT);

        for (int i = 0; i < 2; i++) {

            marketData.addMdIncGroup(Fix.MdIncGrp.newBuilder()
                    .setTradeId(1234L)
                    .setSecurityId(56789L)
                    .setMdEntryPx(Fix.Decimal64.newBuilder().setMantissa(50).build())
                    .setMdEntrySize(Fix.IntQty32.newBuilder().setMantissa(10).build())
                    .setNumberOfOrders(1)
                    .setMdUpdateAction(Fix.MdIncGrp.MdUpdateAction.NEW)
                    .setRepSeq(1)
                    .setAggressorSide(Fix.MdIncGrp.Side.BUY)
                    .setMdEntryType(Fix.MdIncGrp.MdEntryType.BID)
                    .build());

        }

        return marketData;
    }

}
