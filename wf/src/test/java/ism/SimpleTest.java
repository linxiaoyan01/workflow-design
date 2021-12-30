package ism;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SimpleTest {


    @Test
    public void testHello() {
        Func<String> outHelloFunc = new Func<String>("outHelloFunc") {
            @Override
            public void eventHandle(FuncDescriptor funcDescriptor, State<String> nextState) {
            }

            @Override
            public void transfer(FuncDescriptor funcDescriptor, State<String> nextState) {
                nextState.payload="hello world!";
            }
        };


        Funcs<String> funcs = new Funcs<>();
        funcs.addNode(outHelloFunc);

        State<String> state=new State<>();
        state.tobeStarted.add(new FuncDescriptor("outHelloFunc"));


        Case<String> aCasecase = new Case<>();
        aCasecase.funcs=funcs;
        aCasecase.start(state);

        assertEquals("hello world!",state.payload);

    }

    @Test
    public void testHelloForTwo() {
        Func<String> outHelloFunc = new Func<String>("outHelloFunc") {
            @Override
            public void eventHandle(FuncDescriptor funcDescriptor, State<String> nextState) {
            }

            @Override
            public void transfer(FuncDescriptor funcDescriptor, State<String> nextState) {
                nextState.payload="hello";
                //生成下一个节点
                nextState.tobeStarted.add(new FuncDescriptor("outWorldFunc"));
            }
        };

        Func<String> outWorldFunc = new Func<String>("outWorldFunc") {
            @Override
            public void eventHandle(FuncDescriptor funcDescriptor, State<String> nextState) {
            }

            @Override
            public void transfer(FuncDescriptor funcDescriptor, State<String> nextState) {
                nextState.payload+=" world!";
            }
        };


        Funcs<String> funcs = new Funcs<>();
        funcs.addNode(outHelloFunc);
        funcs.addNode(outWorldFunc);

        State<String> state=new State<>();
        state.tobeStarted.add(new FuncDescriptor("outHelloFunc"));


        Case<String> aCasecase = new Case<>();
        aCasecase.funcs=funcs;
        aCasecase.start(state);

        assertEquals("hello world!",state.payload);

    }

    @Test
    public void testHelloFromIng() {
        Func<String> outHelloFunc = new Func<String>("outHelloFunc") {
            @Override
            public void eventHandle(FuncDescriptor funcDescriptor, State<String> nextState) {
                //消息来了,唤醒自己
                assertEquals("msgName",nextState.toIngMsg.name);
                nextState.payload="hello world!";
            }

            @Override
            public void transfer(FuncDescriptor funcDescriptor, State<String> nextState) {
                //睡眠
                nextState.ing.add(funcDescriptor);
            }
        };


        Funcs<String> funcs = new Funcs<>();
        funcs.addNode(outHelloFunc);

        State<String> state=new State<>();
        state.tobeStarted.add(new FuncDescriptor("outHelloFunc"));


        Case<String> aCasecase = new Case<>();
        aCasecase.funcs=funcs;
        aCasecase.start(state);
        assertNull(state.payload);

        aCasecase.transfer(theState -> theState.toIngMsg=new Msg<>("msgName",new Object()));
        aCasecase.resume();
        assertEquals("hello world!",state.payload);

    }

}
