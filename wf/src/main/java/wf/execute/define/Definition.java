package wf.execute.define;

import ism.Funcs;
import wf.Data;

import java.util.LinkedHashSet;
import java.util.Set;

public class Definition extends Funcs<Data> {
    public Set<Transition> transtions = new LinkedHashSet<>();

    public Definition build() {

        id2Func.forEach((k, v) -> {
            Node node = (Node)v;
            node.outGoings.clear();
            node.inComings.clear();
        });

        //build nodes net
        transtions.forEach(trans -> {
            Node source = (Node) node(trans.from);
            Node target = (Node) node(trans.to);
            if (source != null) source.outGoings.add(trans);
            else throw new RuntimeException("cann't find source node!");
            if (target != null) target.inComings.add(trans);
            else throw new RuntimeException("cann't find target node!");
        });

        return this;
    }
}
