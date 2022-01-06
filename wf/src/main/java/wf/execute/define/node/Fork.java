package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.ForkToken;
import wf.ForkTokenManager;
import wf.IExprEval;
import wf.execute.define.Node;
import wf.execute.define.Transition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Fork extends Node {


    //条件表达式求值接口
    private IExprEval exprEval;

    public Fork(String id, IExprEval exprEval) {
        super(id);
        this.exprEval = exprEval;
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Data> nextState) {
        ForkTokenManager forkTokenManager = ForkTokenManager.instance(nextState);

        //得到所有边上表达式为true的出边
        List<Transition> trueOutGoingTransitions = outGoings.stream()
                .filter(transition -> exprEval.eval(transition.condition))
                .collect(Collectors.toList());

        //没有合适的出边，则报错
        if (trueOutGoingTransitions.size() == 0)
            throw new RuntimeException("all outGoings are false!!");


        //得到本节点上所有的token
        Set<ForkToken> nodeTokens = forkTokenManager.nodeTokens(id);

        trueOutGoingTransitions
                .forEach(transition -> {
                    //把出边对应的终止节点加入到调度
                    nextState.tobeStarted.add(new FuncDescriptor(transition.to));
                    //传递本fork产生的token
                    forkTokenManager.addToken(transition.to, id, transition.from + "_" + transition.to);
                    //传递非本fork产生的token
                    forkTokenManager.nodeTokens(transition.to).addAll(nodeTokens);
                });

        //清空本节点持有的token
        forkTokenManager.nodeTokens(id).clear();
    }

}
