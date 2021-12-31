package wf.execute.define;

import ism.Func;
import wf.Data;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class Node2 extends Func<Data> {

    //出边集合
    public Set<Transition2> outGoings = new LinkedHashSet<>();

    //入边集合
    public Set<Transition2> inComings = new LinkedHashSet<>();

    //存储节点配置信息
    protected Map<String, Object> options = new HashMap<>();

    public Node2(String id) {
        super(id);
    }
}
