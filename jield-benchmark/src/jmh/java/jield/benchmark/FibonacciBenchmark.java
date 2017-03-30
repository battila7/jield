package jield.benchmark;

import jield.examples.Fibonacci;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(5)
public class FibonacciBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        Fibonacci fibonacci = new Fibonacci();

        @Param({"1", "10", "100"})
        long limit;
    }

    @Benchmark
    public void jield(BenchmarkState state, Blackhole bh) {
        state.fibonacci
                .fibJield()
                .limit(state.limit)
                .forEach(bh::consume);
    }

    @Benchmark
    public void streamGenerate(BenchmarkState state, Blackhole bh) {
        state.fibonacci
                .fibStreamGenerate()
                .limit(state.limit)
                .forEach(bh::consume);
    }
}
