package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wf.Data;
import wf.ForkToken;
import wf.ForkTokenManager;
import wf.execute.define.Node2;
import wf.execute.define.Transition2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Join2 extends Node2 {

    public static final String KEY_FORK_ID="KEY_FORK_ID";

    public Join2(String id, Map<String, Object> options) {
        super(id);
        this.options.putAll(options);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Data> nextState) {
        Transition2 next = next(nextState);
        if (next != null)
            nextState.tobeStarted.add(new FuncDescriptor(next.to));
    }


    protected Transition2 next(State<Data> returnState) {
        ForkTokenManager forkTokenManager = ForkTokenManager.instance(returnState);

        String forkID = (String) options.get(KEY_FORK_ID);

        //所有的token都持有
        if (!forkTokenManager.allInOneNode(forkID, id)) return null;

        //清空对应的fork的所有token
        //清空后，本节点就不在持有对应的fork的任何token
        forkTokenManager.removeToken(forkID);

        Transition2 next = outGoings.iterator().next();

        //传递到下游
        forkTokenManager.pass2Node(id, next.to);

        return next;
    }
}
