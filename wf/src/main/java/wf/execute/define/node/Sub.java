package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.State;
import wf.Data;
import wf.execute.Instance;
import wf.execute.define.Definition;
import wf.execute.define.Transition;

import java.util.UUID;

public class Sub extends Activity {

    //子流程流程定义
    private Definition definition;

    public static final String KEY_CHILDINSTANCE_ID = "KEY_CHILDINSTANCE_ID";

    public Sub(String id, Definition definition) {
        super(id);
        this.definition = definition;
    }

    @Override
    protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
        //第一次被调度
        if (onEnter) {

            //初始化一个新的子流程实例
            Instance childInstance = initNewChildInstance(funcDescriptor);

            //生成传递给子流程的业务数据
            Data data = childInstanceInitalData(nextState.payload);

            //启动子流程实例
            childInstance.start(data);

            /* 子流程运行中 ...... */

            //子流程很快运行完毕
            if (childInstance.isComplete()) {
                //善后处理
                doAfterChildFinished(funcDescriptor, nextState);
                //接着往下走
                return outGoings.iterator().next();
            } else {
                //子流程运行没有结束，处于休眠状态
                return null;
            }

        }

        //第二次被调起，从休眠中
        else {
            //善后处理
            doAfterChildFinished(funcDescriptor, nextState);
            //接着往下走
            return outGoings.iterator().next();
        }
    }


    public Data childInstanceInitalData(Data other) {
        Data data = new Data();
        data.putAll(other);

        //只传递业务数据，状态控制数据不传
        data.remove(Data.MAP_KEY_FORKTOKENTS_MAP);
        data.remove(Data.MAP_KEY_MSG_HISTORY);

        return data;
    }

    public void childData2Parent(Data child, Data parent) {

        //只传递业务数据，状态控制数据不传
        child.remove(Data.MAP_KEY_FORKTOKENTS_MAP);
        child.remove(Data.MAP_KEY_MSG_HISTORY);

        parent.putAll(child);
    }

    private Instance initNewChildInstance(FuncDescriptor funcDescriptor) {
        try {


            Instance childInstance = new Instance(UUID.randomUUID().toString(), definition);

            //维护父子关系
            childInstance.parent = instance;
            instance.childRen.add(childInstance);

            //存储本节点的那个子流程实例ID
            funcDescriptor.getOptions().put(KEY_CHILDINSTANCE_ID, childInstance.getId());

            return childInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void doAfterChildFinished(FuncDescriptor funcDescriptor, State<Data> returnState) {
        //恢复子流程实例
        Instance childInstance = loadChildInstance(funcDescriptor);
        //子流程传递业务数据到父流程
        childData2Parent(childInstance.latestState().payload, returnState.payload);
        //移除父子关系
        instance.childRen.remove(childInstance);
        childInstance.parent = null;
    }

    private Instance loadChildInstance(FuncDescriptor funcDescriptor) {

        //得到本节点的那个子流程实例ID
        String childInstanceID = (String) funcDescriptor.getOptions().get(KEY_CHILDINSTANCE_ID);

        //从流程实例中，得到本节点的那个子流程实例
        return instance.childRen.stream()
                .filter(pvmInstance1 -> pvmInstance1.getId().equals(childInstanceID))
                .findAny()
                .orElse(null);
    }

}
