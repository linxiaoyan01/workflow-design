package ism;

import java.util.function.Consumer;


/**
 * @param <T> T表示State负载的类型
 */
public class Case<T> {

    public Funcs<T> funcs;

    public State<T> currentState;

    //主循环逻辑
    private void fireNextFunc() {

        //优先处理toIngMsg
        if (currentState.toIngMsg != null) {


            //没有ing的Func需要处理toIngMsg
            if (currentState.ing.size() == 0) {
                //防止死循环
                currentState.toIngMsg = null;
                return;
            }

            //找到一个没有处理过的ing的Func
            FuncDescriptor descriptor = currentState.ing.stream()
                    .filter(funcDescriptor -> !currentState.alreadySend.contains(funcDescriptor))
                    .findFirst()
                    .orElse(null);

            if (descriptor != null) {

                //调起Func来处理toIngMsg，传入State为了方便获取其他信息
                funcs.node(descriptor.funcID).eventHandle(descriptor, currentState);
                //放入已处理集合
                currentState.alreadySend.add(descriptor);

                //如果所有的ing都被处理过，进行一些清除操作
                if (currentState.alreadySend.containsAll(currentState.ing)) {
                    currentState.toIngMsg = null;
                    currentState.alreadySend.clear();
                }
            }
        } else {
            //正常处理tobestarted
            FuncDescriptor descriptor = currentState.tobeStarted.stream()
                    .findFirst()
                    .orElse(null);

            if (descriptor != null) {

                //调起Func来处理State
                funcs.node(descriptor.funcID).transfer(descriptor, currentState);

                //处理完毕，移除队列
                currentState.tobeStarted.remove(descriptor);
            }
        }

    }


    /**
     * 外部干预State
     */
    public void transfer(Consumer<State<T>> consumerState) {
        consumerState.accept(currentState);
    }

    //开始处理
    public void start(State<T> state) {
        this.currentState = state;
        resume();
    }

    public void resume() {
        schedule();
    }

    private void schedule() {
        //loop until noneToSchedule
        while (!currentState.noneToSchedule()) {
            fireNextFunc();
        }
    }

}
