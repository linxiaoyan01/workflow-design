package ism;

/**
 * @param <T> T表示负载的类型
 */
public abstract class Func<T> {

    //唯一标识本转换
    public String id;

    public Func(String id) {
        this.id = id;
    }

    /**
     * 此方法主要用于处理toIngMsg消息
     */
    public abstract void eventHandle(FuncDescriptor funcDescriptor, State<T> nextState);

    /**
     * 正常调度时，被调起
     */
    public abstract void transfer(FuncDescriptor funcDescriptor, State<T> nextState);
}
