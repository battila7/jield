package jield.examples;

import jield.annotation.Generator;

import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Fibonacci {
    public static void main(String[] args) {
        Fibonacci f = new Fibonacci();

        f.fibJield()
                .limit(20)
                .forEach(System.out::println);

        f.fibStandard()
                .limit(20)
                .forEach(System.out::println);
    }

    @Generator
    private Stream<Integer> fibJield() {
        int a = 0, b = 1;

        while (true) {
            int temp = a;
            a = b;
            b = a + temp;

            return temp;
        }
    }

    private IntStream fibStandard() {
        return IntStream.generate(new IntSupplier() {
            private int a = 0;

            private int b = 1;

            @Override
            public int getAsInt() {
                int temp = a;
                a = b;
                b = a + temp;

                return temp;
            }
        });
    }
}
