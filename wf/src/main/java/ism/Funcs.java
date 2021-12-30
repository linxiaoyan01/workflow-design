package ism;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换集合，用于维护所有的转换
 * @param <T> T表示State负载的类型
 */
public class Funcs<T> {
    public String id;

    public Map<String, Func<T>> id2Func = new HashMap<>();

    public Funcs(String id) {
        this.id = id;
    }

    public Funcs() {
    }


    //不能重复添加已经存在的Func
    public void addNode(Func<T> func) {

        if (id2Func.get(func.id) != null)
            throw new RuntimeException("Can't add an exist Func!");

        id2Func.put(func.id, func);
    }

    public void removeNode(String funcID) {
        id2Func.remove(funcID);
    }

    public Func<T> node(String id) {
        return id2Func.get(id);
    }
}
