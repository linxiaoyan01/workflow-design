package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.ForkTokenManager;
import wf.execute.define.Node;
import wf.execute.define.Transition;

import java.util.Map;

public class Join extends Node {

    private final String forkID;

    public Join(String id, String forkID) {
        super(id);
        this.forkID=forkID;
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Data> nextState) {
        Transition next = next(nextState);
        if (next != null)
            nextState.tobeStarted.add(new FuncDescriptor(next.to));
    }


    protected Transition next(State<Data> returnState) {
        ForkTokenManager forkTokenManager = ForkTokenManager.instance(returnState);

        //所有的token都持有
        if (!forkTokenManager.allInOneNode(forkID, id)) return null;

        //清空对应的fork的所有token
        //清空后，本节点就不在持有对应的fork的任何token
        forkTokenManager.removeToken(forkID);

        Transition next = outGoings.iterator().next();

        //传递到下游
        forkTokenManager.pass2Node(id, next.to);

        return next;
    }
}
