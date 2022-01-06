package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.Msg;
import ism.State;
import wf.Data;
import wf.execute.define.Transition;

import java.util.List;

public class Tx extends Activity {


    private final Msg<?> msg;

    public Tx(String id, Msg<?> msg) {
        super(id);
        this.msg=msg;
    }

    @Override
    protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
        nextState.toIngMsg = msg;

        //放入历史
        List<Msg<?>> msgHistory = nextState.payload.getMsgHistory();
        msgHistory.add(nextState.toIngMsg);

        return outGoings.iterator().next();
    }

}
