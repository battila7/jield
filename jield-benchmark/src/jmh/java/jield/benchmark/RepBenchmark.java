package jield.benchmark;

import jield.examples.Rep;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(5)
public class RepBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        Rep rep = new Rep();

        List<String> lst;

        @Param({"1", "10", "100"})
        int times;

        @Param({"1", "10", "100"})
        int each;

        @Setup
        public void setup() {
            lst = Arrays.asList("First", "Second", "Third");
        }
    }

    @Benchmark
    public void jield(BenchmarkState state, Blackhole bh) {
        state.rep
                .repJield(state.lst, state.times, state.each)
                .forEach(bh::consume);
    }

    @Benchmark
    public void streamGenerate(BenchmarkState state, Blackhole bh) {
        state.rep
                .repStreamGenerate(state.lst, state.times, state.each)
                .forEach(bh::consume);
    }
}
