package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.ForkTokenManager;
import wf.execute.define.Node;
import wf.execute.define.Transition;

public abstract class Activity extends Node {

    public Activity(String id) {
        super(id);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Data> nextState) {
        //处理业务
        Transition next = process(funcDescriptor, nextState, true);

        //处理完毕
        if (next != null) {
            next(nextState, next);
        } else {
            //未处理完毕，加入到进行中集合
            if (nextState.tobeStarted.contains(funcDescriptor))
                nextState.ing.add(funcDescriptor);
        }
    }


    //准备调起下一个节点
    protected void next(State<Data> nextState, Transition next) {
        ForkTokenManager forkTokenManager = ForkTokenManager.instance(nextState);
        //传递token
        forkTokenManager.pass2Node(id, next.to);

        nextState.tobeStarted.add(new FuncDescriptor(next.to));
    }

    /**
     *
     * @param funcDescriptor 描述
     * @param nextState 最新的状态
     * @param onEnter true : 第一次被调度，也就是处理业务 ，false : 第一次调度没有处理完，经过长时间的处理，处理完毕后
     * @return 出边 ：null表示处理中，非null表示出边是哪个
     */
    protected abstract Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter);


    /**
     * 长时长逻辑处理完毕，需要调此方法，让流程继续往下走
     * @param funcDescriptor
     * @param nextState
     */
    public void onSubmit(FuncDescriptor funcDescriptor, State<Data> nextState) {

        Transition next = process(funcDescriptor, nextState, false);
        if (next != null) {
            next(nextState, next);
            nextState.ing.remove(funcDescriptor);
        }
    }

}
