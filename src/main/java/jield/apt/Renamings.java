package jield.apt;

import java.util.HashMap;
import java.util.Map;

final class Renamings {
    private final Map<String, String> renamingMap;

    static Renamings empty() {
        return new Renamings();
    }

    private Renamings() {
        this.renamingMap = new HashMap<>();
    }

    Renamings rename(String from, String to) {
        final Renamings r = new Renamings();

        r.renamingMap.putAll(this.renamingMap);

        r.renamingMap.put(from, to);

        return r;
    }
}
