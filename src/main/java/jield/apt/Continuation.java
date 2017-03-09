package jield.apt;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

final class Continuation {
    static final String NO_LABEL = null;

    private final Map<String, Integer> breakMap;

    private final Map<String, Integer> continueMap;

    private final Map<String, String> renamingMap;

    private final List<String> labels;

    private final int nextCont;

    static Continuation empty() {
        return new Continuation(emptyMap(), emptyMap(), emptyMap(), emptyList(), -1);
    }

    private Continuation(Map<String, Integer> breakMap, Map<String, Integer> continueMap,
                         Map<String, String> renamingMap, List<String> labels, int nextCont) {
        this.breakMap = new HashMap<>(breakMap);

        this.continueMap = new HashMap<>(continueMap);

        this.renamingMap = new HashMap<>(renamingMap);

        this.labels = new ArrayList<>(labels);

        this.nextCont = nextCont;
    }

    Continuation nextCont(int cont) {
        return new Continuation(breakMap, continueMap, renamingMap, labels, cont);
    }

    Continuation breakCont(String label, int cont) {
        final Continuation c = new Continuation(breakMap, continueMap, renamingMap, labels, cont);

        c.breakMap.put(label, cont);

        return c;
    }

    Continuation continueCont(String label, int cont) {
        final Continuation c = new Continuation(breakMap, continueMap, renamingMap, labels, cont);

        c.continueMap.put(label, cont);

        return c;
    }

    Continuation rename(String from, String to) {
        final Continuation c = new Continuation(breakMap, continueMap, renamingMap, labels, nextCont);

        c.renamingMap.put(from, to);

        return c;
    }

    Continuation label(String l) {
        final Continuation c = new Continuation(breakMap, continueMap, renamingMap, labels, nextCont);

        c.labels.add(l);

        return c;
    }

    Continuation clearLabels() {
        return new Continuation(breakMap, continueMap, renamingMap, emptyList(), nextCont);
    }

    int getNextCont() {
        return nextCont;
    }

    int getBreakCont(String label) {
        return breakMap.get(label);
    }

    int getContinueCont(String label) {
        return continueMap.get(label);
    }

    List<String> getLabels() {
        return labels;
    }

    String nameOf(String variable) {
        return renamingMap.getOrDefault(variable, variable);
    }
}
