package jield.examples.scheduler;

import java.util.stream.Stream;

public interface Microthread {
    Stream<Signal> execute();
}
