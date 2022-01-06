package wf.execute.define.node;

import ism.FuncDescriptor;
import ism.Msg;
import ism.State;
import wf.Data;
import wf.execute.define.Transition;

import java.util.List;
import java.util.Map;


public class Rx extends Activity {

    //监听模式
    public static final String KEY_MODEL = "KEY_MODEL";
    public static final String KEY_MSG = "KEY_MSG";
    //监听历史
    public static final String MODEL_LISTEN_AND_FIND = "MODEL_LISTEN_AND_FIND";
    //监听未来
    public static final String MODEL_LISTEN_ONLY = "MODEL_LISTEN_ONLY";

    public Rx(String id, Map<String, Object> options) {
        super(id);
        this.options.putAll(options);
    }


    //是否为我感兴趣的消息存在判断
    protected boolean isMyMsg(FuncDescriptor funcDescriptor, Msg<?> msg) {
        return msg.name.equals(options.get(KEY_MSG));
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {
        if (isMyMsg(funcDescriptor, nextState.toIngMsg)) {
            Transition next = outGoings.iterator().next();
            next(nextState, next);
            nextState.ing.remove(funcDescriptor);
        }

    }

    @Override
    protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
        //监听未来
        if (MODEL_LISTEN_ONLY.equals(options.get(KEY_MODEL))) {
            return null;
        }

        //监听历史
        List<Msg<?>> msgHistory = nextState.payload.getMsgHistory();
        //历史中查找消息
        Msg<?> msg = msgHistory.stream()
                .filter(x -> isMyMsg(funcDescriptor, x))
                .findAny()
                .orElse(null);

        //找不到，则继续监听未来
        if (msg == null) {
            return null;
        }

        //找到则继续往下走
        return outGoings.iterator().next();
    }
}
