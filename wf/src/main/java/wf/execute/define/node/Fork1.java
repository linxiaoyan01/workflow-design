package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.execute.define.Node1;

public class Fork1 extends Node1 {

    public Fork1(String id) {
        super(id);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Object> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Object> nextState) {
        outGoings.forEach(transition -> {
            nextState.tobeStarted.add(new FuncDescriptor(transition.to));
        });
    }

}
