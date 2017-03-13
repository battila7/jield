package jield.examples;

import jield.annotation.Generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Rep {
    public static void main(String[] args) {
        Rep r = new Rep();

        r.repJield(Arrays.asList("Hello", "World", "!"), 2, 2)
                .forEach(System.out::println);

        r.repStandard(Arrays.asList("Hello", "World", "!"), 2, 2)
                .forEach(System.out::println);
    }

    @Generator
    private <T> Stream<T> repJield(Iterable<T> iter, int times, int each) {
        for (int j = 0; j < times; ++j) {
            for (T element : iter) {
                for (int i = 0; i < each; ++i) {
                    return element;
                }
            }
        }
    }

    private <T> Stream<T> repStandard(Iterable<T> iter, int times, int each) {
        return Stream.generate(() -> StreamSupport.stream(iter.spliterator(), false)
                                        .map(t -> Collections.nCopies(each, t))
                                        .flatMap(Collection::stream))
                .limit(times)
                .flatMap(Function.identity());
    }
}
