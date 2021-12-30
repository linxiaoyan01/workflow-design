package ism;

public class Msg<T> {
    public String name;
    public T param;

    public Msg() {
    }

    public Msg(String name, T param) {
        this.name = name;
        this.param = param;
    }
}
