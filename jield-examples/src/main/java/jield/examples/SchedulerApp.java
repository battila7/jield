package jield.examples;

import jield.annotation.Generator;
import jield.examples.scheduler.Microthread;
import jield.examples.scheduler.SchedulableMicrothread;
import jield.examples.scheduler.Scheduler;
import jield.examples.scheduler.Signal;

import java.util.stream.Stream;

import static jield.examples.scheduler.Signal.CONTINUE;
import static jield.examples.scheduler.Signal.continueWith;

public class SchedulerApp {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();

        scheduler.addThread(SchedulableMicrothread.of(new EvenCounter())
                            .withIdentifier("even")
                            .build());

        scheduler.addThread(SchedulableMicrothread.of(new OddCounter())
                            .withIdentifier("odd")
                            .build());

        scheduler.beginExecution();
    }

    public static class EvenCounter implements Microthread {
        @Override
        @Generator
        public Stream<Signal> execute() {
            for (int i = 0; i < 10; i += 2) {
                System.out.println(i);
                return continueWith("odd");
            }
        }
    }

    public static class OddCounter implements Microthread {
        @Override
        @Generator
        public Stream<Signal> execute() {
            for (int i = 1; i < 10; i += 2) {
                System.out.println(i);
                return continueWith("even");
            }
        }
    }
}
