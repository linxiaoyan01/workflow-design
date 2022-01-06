package wf.execute;

import ism.*;
import org.junit.Test;
import wf.Data;
import wf.IExprEval;
import wf.execute.define.Definition;
import wf.execute.define.Transition;
import wf.execute.define.node.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SimpleTest {

//#### 1.2.1 Activity
//
//#### 1.2.2 Fork-Join
//
//#### 1.2.3 Rx-Tx
//
//#### 1.2.4 Sub

    @Test
    public void testActivity() {

        Definition definition = new Definition();
        definition.addNode(new Start("start"));
        definition.addNode(new Activity("hello") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("STR", "hello world!");
                return outGoings.iterator().next();
            }
        });
        definition.addNode(new End("end"));

        definition.transtions.add(new Transition("start", "hello", null));
        definition.transtions.add(new Transition("hello", "end", null));

        definition.build();

        Instance instance = new Instance("instanceID", definition);
        instance.start();

        assertEquals("hello world!", instance.latestState().payload.get("STR"));
    }


    @Test
    public void testForkJoin() {


        Definition definition = new Definition();
        definition.addNode(new Start("start"));
        definition.addNode(new Fork("fork", "true"::equalsIgnoreCase));
        definition.addNode(new Activity("hello1") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("hello1", "hello1");
                return outGoings.iterator().next();
            }
        });
        definition.addNode(new Activity("hello2") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("hello2", "hello2");
                return outGoings.iterator().next();
            }
        });
        definition.addNode(new Activity("hello3") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("hello3", "hello3");
                return outGoings.iterator().next();
            }
        });
        definition.addNode(new Join("join","fork"));
        definition.addNode(new End("end"));

        definition.transtions.add(new Transition("start", "fork", null));
        definition.transtions.add(new Transition("fork", "hello1", "true"));
        definition.transtions.add(new Transition("fork", "hello2", "true"));
        definition.transtions.add(new Transition("fork", "hello3", "false"));
        definition.transtions.add(new Transition("hello1", "join", null));
        definition.transtions.add(new Transition("hello2", "join", null));
        definition.transtions.add(new Transition("hello3", "join", null));
        definition.transtions.add(new Transition("join", "end", null));


        definition.build();

        Instance instance = new Instance("instanceID", definition);
        instance.start();

        assertEquals("hello1", instance.latestState().payload.get("hello1"));
        assertEquals("hello2", instance.latestState().payload.get("hello2"));
        assertFalse(instance.latestState().payload.containsKey("hello3"));
    }

    @Test
    public void testRxTx() {


        Definition definition = new Definition();
        definition.addNode(new Start("start"));
        definition.addNode(new Fork("fork", "true"::equalsIgnoreCase));

        Map<String,Object> rxOption=new HashMap<>();
        rxOption.put(Rx.KEY_MODEL,Rx.MODEL_LISTEN_AND_FIND);
        rxOption.put(Rx.KEY_MSG,"myMsgName");
        definition.addNode(new Rx("rx",rxOption));

        definition.addNode(new Activity("hello1") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("hello1", "hello1");
                return outGoings.iterator().next();
            }
        });

        definition.addNode(new Tx("tx",new Msg<>("myMsgName","helloMsg")));


        definition.addNode(new Join("join","fork"));
        definition.addNode(new End("end"));

        definition.transtions.add(new Transition("start", "fork", null));
        definition.transtions.add(new Transition("fork", "rx", "true"));
        definition.transtions.add(new Transition("rx", "hello1", null));
        definition.transtions.add(new Transition("fork", "tx", "true"));
        definition.transtions.add(new Transition("hello1", "join", null));
        definition.transtions.add(new Transition("tx", "join", null));
        definition.transtions.add(new Transition("join", "end", null));


        definition.build();

        Instance instance = new Instance("instanceID", definition);
        instance.start();

        assertEquals("hello1", instance.latestState().payload.get("hello1"));
    }

    @Test
    public void testSub() {

        Definition definitionChild = new Definition();
        definitionChild.addNode(new Start("start"));
        definitionChild.addNode(new Activity("hello") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("STR_CHILD", "hello world!");
                return outGoings.iterator().next();
            }
        });
        definitionChild.addNode(new End("end"));
        definitionChild.transtions.add(new Transition("start", "hello", null));
        definitionChild.transtions.add(new Transition("hello", "end", null));
        definitionChild.build();

        Definition definitionMain = new Definition();
        definitionMain.addNode(new Start("start"));
        definitionMain.addNode(new Activity("hello") {
            @Override
            protected Transition process(FuncDescriptor funcDescriptor, State<Data> nextState, boolean onEnter) {
                nextState.payload.put("STR_MAIN", "hello world!");
                return outGoings.iterator().next();
            }
        });
        definitionMain.addNode(new Sub("sub",definitionChild));
        definitionMain.addNode(new End("end"));
        definitionMain.transtions.add(new Transition("start", "hello", null));
        definitionMain.transtions.add(new Transition("hello", "sub", null));
        definitionMain.transtions.add(new Transition("sub", "end", null));
        definitionMain.build();

        Instance instance = new Instance("instanceID", definitionMain);
        instance.start();

        assertEquals("hello world!", instance.latestState().payload.get("STR_MAIN"));
        assertEquals("hello world!", instance.latestState().payload.get("STR_CHILD"));
    }

}
