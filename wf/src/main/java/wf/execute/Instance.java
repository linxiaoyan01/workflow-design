package wf.execute;

import ism.*;
import wf.Data;
import wf.execute.define.Definition;
import wf.execute.define.Node;
import wf.execute.define.node.Activity;
import wf.execute.define.node.Start;

import java.util.*;


/**
 * the main class of PVM
 * this class can be serial/deseial with kryo
 */
public class Instance {

    private String id;

    public Instance parent;

    public List<Instance> childRen = new ArrayList<>();

    //使用ISM最为底层核心运行时
    private final Case<Data> aCase = new Case<>();

    public Instance(String id, Definition definition) {
        this.id = id;
        aCase.funcs = definition;
        definition.id2Func.values()
                .forEach(func -> ((Node) func).instance = this);
    }

    public String getId() {
        return id;
    }

    //激活某个休眠中的Activity
    public void submitActivity(FuncDescriptor funcDescriptor) {

        if (!(aCase.funcs.node(funcDescriptor.funcID) instanceof Activity))
            throw new RuntimeException("only Activity can submit!");

        if (!aCase.currentState.ing.contains(funcDescriptor)) {
            throw new RuntimeException("can't find funcDescriptor in ing collection!");
        }

        aCase.transfer((latestState) -> {
            Activity activity = (Activity) aCase.funcs.node(funcDescriptor.funcID);
            //提交
            activity.onSubmit(funcDescriptor, latestState);
        });
        //继续流转
        aCase.resume();
    }

    //发送消息
    public void signal(Msg<?> msg) {
        //直接修改toIngMsg
        aCase.transfer((latestState) -> {
            latestState.toIngMsg = msg;
        });

        //继续流转
        aCase.resume();
    }


    //开始一个流程实例的流转
    public void start() {
        start(null);
    }


    //以指定的数据开始一个流程的流转
    public void start(Data data) {
        if (aCase.currentState != null)
            throw new RuntimeException("Instance has already started!");

        Data dataTemp = data == null ? new Data() : data;

        //初始化payLoad
        State<Data> initialState = new State<>();
        initialState.payload = dataTemp;

        //找到Start节点
        Func<Data> startFunc = aCase.funcs.id2Func.values().stream()
                .filter(func -> Start.class.isAssignableFrom(func.getClass()))
                .findAny().orElse(null);

        if (startFunc == null) throw new RuntimeException("cann't find Start Node!");

        //把Start节点加入到集合中
        initialState.tobeStarted.add(new FuncDescriptor(startFunc.id));

        aCase.start(initialState);
    }


    //取消进行中的某个Activity
    public void cancelIngActivity(FuncDescriptor funcDescriptor) {

        if (!(aCase.funcs.node(funcDescriptor.funcID) instanceof Activity))
            throw new RuntimeException("only Activity can submit!");

        if (!aCase.currentState.ing.contains(funcDescriptor)) {
            throw new RuntimeException("can't find funcDescriptor in ing collection!");
        }

        aCase.transfer((latestState) -> {
            latestState.ing.remove(funcDescriptor);
        });

    }

    //取消整个流程实例的执行
    public void cancel() {
        aCase.transfer(State::setToNoneToDo);
    }

    //得到休眠状态中的Activity
    public List<FuncDescriptor> allIngActivities() {
        return new ArrayList<>(aCase.currentState.ing);
    }

    //流程实例是否已经执行完毕
    public boolean isComplete() {

        /*
            public boolean noneToDo() {
                if (!tobeStarted.isEmpty()) return false;
                return ing.isEmpty();
            }
         */
        return aCase.currentState.noneToDo();
    }

    //当前的最新状态
    public State<Data> latestState() {
        return aCase.currentState;
    }
}
