package wf.execute.define;

import ism.Func;
import wf.Data;
import wf.execute.Instance;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class Node extends Func<Data> {

    //出边集合
    public Set<Transition> outGoings = new LinkedHashSet<>();

    //入边集合
    public Set<Transition> inComings = new LinkedHashSet<>();

    //存储节点配置信息
    protected Map<String, Object> options = new HashMap<>();

    public Instance instance;

    public Node(String id) {
        super(id);
    }
}
