package jield.examples.scheduler;

import java.util.*;

import static jield.examples.scheduler.Signal.CONTINUE;
import static jield.examples.scheduler.Signal.QUIT;
import static jield.examples.scheduler.Signal.REMOVE;

public class Scheduler {
    private final Map<String, SchedulableMicrothread> threadMap;

    public Scheduler() {
        this.threadMap = new HashMap<>();
    }

    public void addThread(SchedulableMicrothread microthread) {
        if (microthread == null) {
            throw new NullPointerException("The microthread must not be null!");
        }

        if (threadMap.containsKey(microthread.getIdentifier())) {
            throw new IllegalArgumentException("A thread with the same identifier is already managed!");
        }

        threadMap.put(microthread.getIdentifier(), microthread);
    }

    public final void beginExecution() {
        Signal previousSignal = CONTINUE;
        SchedulableMicrothread previousThread = null;

        while (!threadMap.isEmpty()) {
            Optional<SchedulableMicrothread> threadOptional = getNextThread(previousThread, previousSignal);

            if (threadOptional.isPresent()) {
                previousThread = threadOptional.get();

                if (previousThread.getThreadIterator().hasNext()) {
                    previousSignal = previousThread.getThreadIterator().next();
                } else {
                    threadMap.remove(previousThread.getIdentifier());

                    previousSignal = CONTINUE;

                    previousThread = null;
                }
            } else {
                return;
            }
        }
    }

    private Optional<SchedulableMicrothread> getNextThread(SchedulableMicrothread previousThread, Signal previousSignal) {
        if (previousSignal == null || previousSignal == QUIT) {
            return Optional.empty();
        } else if (previousSignal == REMOVE) {
            threadMap.remove(previousThread.getIdentifier());
            return selectByPriority();
        } else if (previousSignal == CONTINUE) {
            return selectByPriority();
        } else {
            if (threadMap.containsKey(previousSignal.getIdentifier())) {
                return Optional.of(threadMap.get(previousSignal.getIdentifier()));
            } else {
                return selectByPriority();
            }
        }
    }

    private Optional<SchedulableMicrothread> selectByPriority() {
        SchedulableMicrothread selected = null;

        for (SchedulableMicrothread s : threadMap.values()) {
            if (selected == null || selected.getCurrentPriority() < s.getCurrentPriority()) {
                selected = s;
            }
        }

        for (SchedulableMicrothread s : threadMap.values()) {
            s.setCurrentPriority(s.getCurrentPriority() + 1);
        }

        if (selected != null) {
            selected.setCurrentPriority(selected.getPriority());
        }

        return Optional.ofNullable(selected);
    }
}
