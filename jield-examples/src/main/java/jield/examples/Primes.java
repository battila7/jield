package jield.examples;

import java.util.LinkedList;
import java.util.stream.Stream;
import jield.annotation.Generator;

public class Primes {
    public static void main(String[] args) {
        Primes p = new Primes();

        int i = p.primesJield().limit(10000).reduce(0, (a, b) -> a + b);

        System.out.println(i);
    }

    @Generator
    private Stream<Integer> primesJield() {
        LinkedList<Integer> primes = new LinkedList<>();

        primes.add(2);

        return 2;

        int current = 1;

        loop:
        do {
            current += 2;
            for (int i : primes) {
                if (current % i == 0) {
                    continue loop;
                }
            }

            primes.add(current);

            return current;
        } while (true);
    }
}
