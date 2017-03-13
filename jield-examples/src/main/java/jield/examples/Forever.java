package jield.examples;

import jield.annotation.Generator;

import java.util.stream.Stream;

public class Forever {
    public static void main(String[] args) {
        Forever f = new Forever();

        f.foreverJield("Hello")
                .limit(10)
                .forEach(System.out::println);

        f.foreverStandard("World")
                .limit(10)
                .forEach(System.out::println);
    }

    @Generator
    private <T> Stream<T> foreverJield(T value) {
        while (true) {
            return value;
        }
    }

    private <T> Stream<T> foreverStandard(T value) {
        return Stream.generate(() -> value);
    }
}
