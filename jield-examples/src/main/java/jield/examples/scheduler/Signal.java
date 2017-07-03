package jield.examples.scheduler;

import java.util.Objects;

public final class Signal {
    public static final Signal CONTINUE = new Signal("CONTINUE");

    public static final Signal REMOVE = new Signal("REMOVE");

    public static final Signal QUIT = new Signal("QUIT");

    public static Signal continueWith(String identifier) {
        return new Signal(Objects.requireNonNull(identifier));
    }

    private final String identifier;

    private Signal(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
