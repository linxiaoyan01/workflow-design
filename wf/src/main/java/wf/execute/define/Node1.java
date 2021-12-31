package wf.execute.define;

import ism.Func;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class Node1 extends Func<Object> {

    //出边集合
    public Set<Transition1> outGoings = new LinkedHashSet<>();

    //入边集合
    public Set<Transition1> inComings = new LinkedHashSet<>();

    //存储节点配置信息
    protected Map<String, Object> options = new HashMap<>();

    public Node1(String id) {
        super(id);
    }
}
