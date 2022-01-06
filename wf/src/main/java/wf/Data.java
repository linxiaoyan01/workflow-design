package wf;

import ism.Msg;

import java.util.*;

public class Data extends HashMap<String, Object> implements Map<String, Object> {

    public static final String MAP_KEY_FORKTOKENTS_MAP = "____MAP_KEY_FORKTOKENTS_MAP____";
    public static final String MAP_KEY_MSG_HISTORY = "____MAP_KEY_MSG_HISTORY____";


    public Data() {
    }

    public Map<String, Set<ForkToken>> getForkTokensMap() {
        return (Map<String, Set<ForkToken>>) super.computeIfAbsent(MAP_KEY_FORKTOKENTS_MAP, k -> new HashMap<String, HashSet<ForkToken>>());
    }


    public List<Msg<?>> getMsgHistory() {
        return (List<Msg<?>>) super.computeIfAbsent(MAP_KEY_MSG_HISTORY, k -> new ArrayList<Msg<?>>());
    }

}
