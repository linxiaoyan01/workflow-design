
## 1. ISM 设计与主要实现

按照循序渐进的方式，一步一步的得到最终的设计，
在此基础之上，以部分代码的形式来展示主要的、关键的技术实现细节，这样能够更好地方便读者理解、消化与吸收设计意图。

### 1.1 设计
设计分为几次迭代，每次迭代都是在保留上一次迭代设计的优势的同时，解决遗留下来的一些问题，我们开始吧

#### 1.1.1 主循环调度

什么是主循环调度呢？

我们从无限状态机的运行机制说起，对于初始状态S0，经过一系列的变换T，S0最终变为终态SN，即：不存在一个T，使得SN变为下一个状态。

因此，这里面隐含着一个循环，即：不断的应用T，对状态进行变换，也就是所谓的主循环调度。

主循环调度是无限状态机的最主要的功能之一，因此，这一部分的设计一定要健壮、简单、可维护性强，
这样才能够长时间的运行而不容易出错，简单意味着理解起来简单，才能够在出问题的时候，快速的定位问题，从而进行修复，
主循环依据不同的业务场景，会有不同的变种，因此要强调一定程度上的可维护性。

基本的调度逻辑如下图所示：

![图](http://assets.processon.com/chart_image/61cbcbbff346fb2922b6fc4d.png)

图中的Func队列存储着所有的变化T，一个T记为一个Func，Func以一个State作为输入，经过一系列处理之后，输出另一个State。
主循环逻辑从队列中取出一个Func，判断Func是否存在，如果Func不存在，意味着没有变换需要对State进行处理，故，主循环退出；
如果Func存在，则应用之，对State进行变换，产生一个New State ，然后把这个New State指向State ， 到此 ，一次循环结束。


#### 1.1.2 虚拟中断

主循环逻辑解决了一部分问题，然后，有些转换(Func)需要运行很长时间，显然，这会阻塞主循环，某些情况下是不合理的。
因此，需要在主循环逻辑中加入对长转换(Func)的支撑，怎么支撑呢？我们这里使用了类似于中断的处理逻辑，由于又不是真正的中断，故，命名为虚拟中断。

修改后的调度逻辑如下图所示：

![图](http://assets.processon.com/chart_image/61cbfa351e08532c3ee6a7b5.png?)


这里使用了两个队列，一个是未执行变换的Func队列，另一个是休眠中的Func队列，即：Func Ing队列。
一开始，先从Func Ing队列中取得一个已经休眠的且未被处理过的Func(否则会死循环)，优先进行处理，处理方式同正常处理逻辑，
如果Func Ing是空的，则开始处理Func队列中的变换，与上节一样。
如果Func没有处理完，则需要把Func放入到Func Ing队列，反之，与上节一样。


#### 1.1.3 中断消息

虚拟中断解决了阻塞主循环的问题，然而这又带来了新的问题，每次循环开始，都要对Func Ing队列中的每一个Func进行调起，
这显然是影响调度效率的，尤其是一些超长的转换，几乎绝大多数次调起都是空转，兜兜转转又回到了Func Ing队列中。
为此，进入了中断消息这个机制来解决这个问题。

修改后的调度逻辑如下图所示：

![图](http://assets.processon.com/chart_image/61cc0146e0b34d1be7787aa7.png)


Msg即为中断消息，这个消息存在，意味着需要主循环来处理Func Ing队列，如果不存在这个消息，意味着主循环不需要处理Func Ing队列。
这显然比每次都要遍历Func Ing队列的老方法，效率高的多。
同时，注意到，每次发现Msg消息后，都要把这个消息清空，避免下次循环重复处理，这同样又提高了效率。
其他处理逻辑同上节，此处不再赘述。

#### 1.1.4 统一的State

到此，我们发现，为了解决一个新的问题，我们引入了一个新的解决方案，而该方案又引入了另外一个新的问题，
同时，有一些新的概念被引入，比如：Func队列、Func Ing队列、Msg消息、State等，
这对复杂性的降低是没有任何好处的，是时候该处理这些问题了。
那么如何处理这个复杂性呢？这里我们使用信息隐藏方法，把这些新的概念纳入到另外一个概念下，用新的概念来隐藏这些细节信息。

具体来讲，把原有的State概念扩大，使之包含如下信息：

* payLoad，业务信息、负载，对应于老的State的概念范畴
* tobeStarted，待执行Func队列，一个从来都没有被调起的Func的集合
* ing，执行中Func队列，被调起过，但没有执行完毕需要继续后台执行的Func的集合
* toIngMsg，发送给ing队列中的消息


修改后的调度逻辑如下图所示：

![图](http://assets.processon.com/chart_image/61cc0cdb0e3e74415704d697.png?)

好处如下：

* 逻辑变的简单了，基本就是一句话：优先调度进行中的、然后调度未进行的
* 把tobeStarted、ing、toIngMsg等也当做State来处理，这样，每个Func可以对这些数据进行处理，进而可以控制主循环逻辑的执行

比如，某个Func可以设置toIngMsg，这样在下次循环的时候，就能够让主循环调起ing中的Func；
再比如，某个tobeStarted中的Func在调起执行的时候，发现流程已经结束了，这时它可以清空tobeStarted队列、清空ing队列，这样就能够退出主循环。

当State可以自洽的时候，我们发现，好多问题迎刃而解了。
大道至简、把复杂的问题简单化，这是一个优秀的设计应该具备的一个点。

然而，这样做就没有缺点了吗？任何事物都有两面性，这个也不例外。
这样做的缺点就是太灵活了，如果把握不好，那么有可能会照成逻辑混乱，怎么解决这个问题呢？
这时候就要看应用场景了，技术上应该把这个隐藏到最底层，不要向最终端用户暴露，让有经验的程序员来屏蔽这个缺点。

#### 1.1.5 外部干预

上图中，有一步是“可以被调度？”，当没有toIngMsg且tobeStarted为空的时候，意味着没有可以被调度的Func。
在这种情况下，如果ing也是空的，意味着流程结束了，如果ing不空，那就是流程休眠了，等待唤醒，那么怎么唤醒呢？这就需要外部干预。

具体来讲，需要ISM本身提供一个方法，这个方法可以修改休眠了的State的内容，
一般来讲，调用者都是ing中的Func，因为ing中的Func处理完毕后，需要通知主循环让其继续运行的。
除此方法之外，还需要ISM提供一个resume方法，使得主循环可以依据State的内容，继续运行。


总之，到目前为止，一个基本的ISM设计就算完成了，我们可以看到这样的设计满足一些基本的流程类场景下的需求。
更多的需求场景，可以在此基础上继续进行扩展，因为它的原理足够的简单，理解起来足够的容易，修改起来足够的自信，从而复杂性能够得到很好地把控。


### 1.2 主要实现

有了上面的设计，我们来看看主要的实现，在代码中，如何落地上述设计。

**注：这里只是示例性代码，十分不建议直接用于生产环境。**


#### 1.2.1 状态

```java

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

```

#### 1.2.2 转换

```java

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


```


#### 1.2.3 辅助类

##### 1.2.3.1 消息VO

```java

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

```

##### 1.2.3.2 转换描述符

```java

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
        return Objects.equals(funcID, that.funcID) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(funcID, options);
    }

}

```

##### 1.2.3.3 转换集合

```java

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

```


#### 1.2.4 主类

```java

/**
 * @param <T> T表示State负载的类型
 */
public class Case<T> {

    public Funcs<T> funcs;

    private State<T> currentState;

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

```


### 1.3 示例

接下来用几个示例来说明如何使用上述代码，看完之后，印象会更深刻。

注：为了方便演示和举例，示例代码有些冗余，实际生产环境还需进一步的封装和扩展。

#### 1.3.1 HelloWord

```java

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

```

#### 1.3.2 动态指定下一个要调度的转换

```java

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

```

#### 1.3.3 长转换的睡眠及唤醒

```java

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

```

总结一下，这一章开始主要介绍了ISM的设计，采用了迭代的思想，逐步的把设计演化到了最终的版本，接下来，用代码实现了该设计的主要内容，最后，用了三个测试用例演示了如何使用代码。我相信，经过以上三步，同学们已经能够理解了ISM的原理，并且可以在生产环境中进行灵活的运用了。




