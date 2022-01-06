package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.execute.define.Transition;

public class End extends Activity {

    public End(String id) {
        super(id);
    }

    @Override
    protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
        /*
            nextState.setToNoneToDo(){
                tobeStarted.clear();
                ing.clear();
            }
         */
        nextState.setToNoneToDo();

        //是个子流程
        if (instance.parent != null) {
            //找到休眠中的那个Sub节点
            FuncDescriptor funcDescriptorSub = instance.parent.allIngActivities().stream()
                    .filter(ingFuncDescriptor ->
                            instance.getId().equals(ingFuncDescriptor.getOptions().get(Sub.KEY_CHILDINSTANCE_ID)))
                    .findFirst().orElse(null);


            //提交
            if (funcDescriptorSub != null)
                instance.parent.submitActivity(funcDescriptorSub);
        }

        return null;
    }
}
