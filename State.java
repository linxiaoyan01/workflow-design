package com.jd.jdt.pe.pvm.ism;

import java.util.*;


/***
 * @param <T> T表示负载的类型
 */
public class State<T> {

    //可存储一些其他的业务相关的信息，具体由上层业务指定
    public T payload;

    //需要被调度的转换，FuncDescriptor说明见辅助类小节
    public Set<FuncDescriptor> tobeStarted = new LinkedHashSet<>();

    //进行中的转换
    public Set<FuncDescriptor> ing = new HashSet<>();

    //需要发送给ing的消息，Msg说明见辅助类小节
    public Msg<?> toIngMsg = null;

    //辅助集合，用于保存哪些ing中的被通知过了，即接收过toIngMsg
    public Set<FuncDescriptor> alreadySend = new HashSet<>();


    public boolean noneToSchedule() {
        if (!tobeStarted.isEmpty()) return false;
        return toIngMsg == null;
    }

    public State<T> protoType() {
        State<T> state = new State<>();
        initState(state);
        return state;
    }

    public void initState(State<T> state){
        state.payload = this.payload;

        state.tobeStarted.addAll(this.tobeStarted);
        state.ing.addAll(this.ing);

        state.toIngMsg = this.toIngMsg;
        state.alreadySend.addAll(this.alreadySend);
    }

}
