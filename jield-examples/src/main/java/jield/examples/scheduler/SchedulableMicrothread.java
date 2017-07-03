package jield.examples.scheduler;

import java.util.Iterator;

import static java.util.Objects.isNull;

public class SchedulableMicrothread {
    private final String identifier;

    private final int priority;

    private final Iterator<Signal> threadIterator;

    private int currentPriority;

    public static Builder of(Microthread microthread) {
        return new Builder(microthread);
    }

    private SchedulableMicrothread(String identifier, int priority, Iterator<Signal> threadIterator) {
        this.identifier = identifier;
        this.priority = priority;
        this.threadIterator = threadIterator;
        this.currentPriority = priority;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getPriority() {
        return priority;
    }

    public Iterator<Signal> getThreadIterator() {
        return threadIterator;
    }

    int getCurrentPriority() {
        return this.currentPriority;
    }

    void setCurrentPriority(int priority) {
        this.currentPriority = priority;
    }

    public static class Builder {
        private final Iterator<Signal> threadIterator;

        private int priority;

        private String identifier;

        private Builder(Microthread microthread) {
            this.threadIterator = microthread.execute().iterator();
            this.priority = 0;
        }

        public Builder withPriority(int priority) {
            this.priority = priority;

            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public SchedulableMicrothread build() {
            if (isNull(identifier)) {
                throw new IllegalStateException("The identifier must not be null!");
            }

            if (priority == Integer.MAX_VALUE) {
                throw new IllegalStateException("The priority must be less than Integer.MAX_VALUE!");
            }

            return new SchedulableMicrothread(identifier, priority, threadIterator);
        }
    }
}
