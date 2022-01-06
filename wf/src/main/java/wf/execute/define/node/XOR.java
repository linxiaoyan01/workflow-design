package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.IExprEval;
import wf.execute.define.Transition;

import java.util.List;
import java.util.stream.Collectors;


public class XOR extends Activity {


    //条件表达式求值接口
    private IExprEval exprEval;

    public XOR(String id, IExprEval exprEval) {
        super(id);
        this.exprEval = exprEval;
    }

    @Override
    protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {

        //找到所有可以出的边
        List<Transition> trueOutGoingTransitions = outGoings.stream()
                .filter(transition -> exprEval.eval(transition.condition))
                .collect(Collectors.toList());

        if (trueOutGoingTransitions.size() == 0)
            throw new RuntimeException("at least one outgoing should exist!");

        if (trueOutGoingTransitions.size() > 1)
            throw new RuntimeException("more then one outgoing exist!");

        return trueOutGoingTransitions.get(0);
    }

}
