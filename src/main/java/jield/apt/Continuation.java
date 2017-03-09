package jield.apt;

import java.util.HashMap;
import java.util.Map;

final class Continuation {
    public static final String NO_LABEL = null;

    private final Map<String, Integer> breakMap;

    private final Map<String, String> renamingMap;

    private final String label;

    private final int nextCont;

    static Continuation empty() {
        return new Continuation(new HashMap<>(), new HashMap<>(), NO_LABEL, -1);
    }

    private Continuation(Map<String, Integer> breakMap, Map<String, String> renamingMap, String label, int nextCont) {
        this.breakMap = new HashMap<>();

        this.breakMap.putAll(breakMap);

        this.renamingMap = new HashMap<>();

        this.renamingMap.putAll(renamingMap);

        this.label = label;

        this.nextCont = nextCont;
    }

    Continuation nextCont(int cont) {
        return new Continuation(breakMap, renamingMap, label, cont);
    }

    Continuation breakCont(String label, int cont) {
        final Continuation ctx = new Continuation(breakMap, renamingMap, label, nextCont);

        ctx.breakMap.put(label, cont);

        return ctx;
    }

    Continuation rename(String from, String to) {
        final Continuation ctx = new Continuation(breakMap, renamingMap, label, nextCont);

        ctx.renamingMap.put(from, to);

        return ctx;
    }

    Continuation label(String l) {
        return new Continuation(breakMap, renamingMap, l, nextCont);
    }

    int getNextCont() {
        return nextCont;
    }

    int getBreakCont(String label) {
        return breakMap.get(label);
    }

    String getLabel() {
        return label;
    }

    String nameOf(String variable) {
        return renamingMap.getOrDefault(variable, variable);
    }
}
