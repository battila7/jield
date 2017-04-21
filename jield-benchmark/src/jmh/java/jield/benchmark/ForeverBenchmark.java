package jield.benchmark;

import jield.examples.Forever;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(5)
public class ForeverBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        Forever forever = new Forever();

        String value = "Test String";

        @Param({"1", "10", "100"})
        long limit;
    }

    @Benchmark
    public void jield(BenchmarkState state, Blackhole bh) {
        state.forever
                .foreverJield(state.value)
                .limit(state.limit)
                .forEach(bh::consume);
    }

    @Benchmark
    public void streamGenerate(BenchmarkState state, Blackhole bh) {
        state.forever
                .foreverStreamGenerate(state.value)
                .limit(state.limit)
                .forEach(bh::consume);
    }
}
