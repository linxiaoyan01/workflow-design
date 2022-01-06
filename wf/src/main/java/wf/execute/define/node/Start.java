package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.execute.define.Transition;

public class Start extends Activity {

    public Start(String id) {
        super(id);
    }

    @Override
    protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter){
        return outGoings.iterator().next();
    }
}
