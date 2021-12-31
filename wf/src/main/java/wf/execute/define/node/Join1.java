package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Const;
import wf.execute.define.Node1;

public class Join1 extends Node1 {

    public Join1(String id, int count) {
        super(id);
        //记录对应Fork的出边条数
        options.put("ForkOutGoingsCount", count);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Object> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Object> nextState) {
        //每次减一
        Integer count = (Integer) options.get("ForkOutGoingsCount");
        count--;

        //减到零，意味着同步完成，流程接着往下走
        if (count == 0)
            nextState.tobeStarted.add(new FuncDescriptor(outGoings.iterator().next().to));
        else
            options.put("ForkOutGoingsCount", count);
    }

}
