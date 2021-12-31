package wf;

import java.util.*;

public class Data extends HashMap<String, Object> implements Map<String, Object> {

    private static final String MAP_KEY_FORKTOKENTS_MAP = "____MAP_KEY_FORKTOKENTS_MAP____";

    public Data() {
    }

    public Map<String, Set<ForkToken>> getForkTokensMap() {
        return (Map<String, Set<ForkToken>>) super.computeIfAbsent(MAP_KEY_FORKTOKENTS_MAP, k -> new HashMap<String, HashSet<ForkToken>>());
    }
}
