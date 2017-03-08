package jield.apt;

import java.util.HashMap;
import java.util.Map;

final class Continuations {
    private final Map<String, Integer> breakMap;

    private final Map<String, Integer> continueMap;

    private final int nextCont;

    static Continuations endingAt(int cont) {
        return new Continuations(new HashMap<>(), new HashMap<>(), cont);
    }

    private Continuations(Map<String, Integer> breakMap, Map<String, Integer> continueMap, int nextCont) {
        this.breakMap = new HashMap<>();

        this.breakMap.putAll(breakMap);

        this.continueMap = new HashMap<>();

        this.continueMap.putAll(continueMap);

        this.nextCont = nextCont;
    }

    Continuations next(int cont) {
        return new Continuations(breakMap, continueMap, cont);
    }

    Continuations addBreak(String label, int cont) {
        final Continuations c = new Continuations(breakMap, continueMap, nextCont);

        c.breakMap.put(label, cont);

        return c;
    }

    Continuations addContinue(String label, int cont) {
        final Continuations c = new Continuations(breakMap, continueMap, nextCont);

        c.continueMap.put(label, cont);

        return c;
    }

    int getNext() {
        return nextCont;
    }

    int getBreak(String label) {
        return breakMap.get(label);
    }

    int getContinue(String label) {
        return continueMap.get(label);
    }
}
