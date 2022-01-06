package ism;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * 因为一个转换可能被多次调起，所以需要对此进行区分，使用本结构体来描述一次调起
 */
public class FuncDescriptor {

    //转换标识
    public String funcID;

    //其他附加信息
    private Map<String, Object> options = new HashMap<>();


    public FuncDescriptor(String funcID) {
        this.funcID = funcID;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuncDescriptor that = (FuncDescriptor) o;
        return Objects.equals(funcID, that.funcID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(funcID);
    }

}
