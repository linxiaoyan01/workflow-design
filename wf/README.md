# 1. 工作流引擎执行服务的设计与主要实现

工作流引擎执行服务是工作流引擎的核心，可以说，在某些场景下，工作流引擎就是工作流引擎执行服务，这一部分的好坏对一个工作流引擎是至关重要的。
这一章介绍如何设计一个实用的工作流引擎执行服务，并给出主要实现。

## 1.1 设计

工作流引擎执行服务一般分为两大部分，一个是流程定义，一个是流程执行，及其由它们引出的其他配套机制。
接下来，分别进行介绍。


### 1.1.1 流程定义

流程定义，即：定义一个流程的流转过程。

宏观上讲，一个流程是由流程节点及节点之间的走向决定的，如下图所示：

![图](http://assets.processon.com/chart_image/61cd5491e0b34d1be77bd476.png?)

其中，N1,N2,N3,N4,N5为节点，E1,E2,E3,E4,E5为边，流程的流转由开始节点(N1)开始，按照边的箭头方向往下流转，直达结束节点(N5)。

进一步的，我们暂且把某个节点流出的边成为该节点的出边，反之为入边，例如：N1的出边为E1、E1为N1的出边、E1为N2的入边、N2的两个入边为E1和E2、N5没有出边、N5只有一个入边E5。

一般来说，只有出边没有入边的点，我们称之为开始节点，流程的流转从开始节点开始；
相应的，只有入边而没有出边的点，我们称之为结束节点，流程的流转在结束节点结束。


#### 1.1.1.1 节点

如何设计节点呢？根据常识判断，一定会有很多种不同类型的节点，每种节点有不同的概念范畴、属性和行为，但无论如何，它们还是有一些共同的东西的，因此，我们可以首先设计一个抽象的节点，用于据此派生出各种其他节点。

那么，这个抽象节点包含哪些信息呢？直观上讲，从图中可以发现，节点包含边，因此我们用一个集合来表示这个节点所有的边。

从另一个角度讲，出边和入边的应用场景是有很大不同的，如：出边一般来讲经常用于流转相关的场景，入边一般用于回溯相关的场景。故此，我们要把边分为两类，用两个集合表示，一个是出边的集合，一个是入边的集合。

从大的维度来讲，一次流程的运行就是对一个状态的处理，流程中的每一次节点遍历就是对状态的一个转换，因此，我们可以使用ISM相关的理论，把一个节点看为是一个Func，即：一个转换。

这样，节点派生自Func。


另外，节点一定有一些自己的配置信息，这些信息可以存储在一个Map结构的成员变量里面。

暂时(代码随着设计的深入，会不断的进化，不是最终版本)用代码示意如下：

```java

public abstract class Node extends Func<Object> {

    //出边集合
    public Set<Transition> outGoings = new LinkedHashSet<>();

    //入边集合
    public Set<Transition> inComings = new LinkedHashSet<>();

    //存储节点配置信息
    protected Map<String, Object> options = new HashMap<>();

    public Node(String id) {
        super(id);
    }
}

```

边用Transition表示，那么它是怎么表示的呢？

#### 1.1.1.2 边

按照节点的设计思路，我们来分析边，发现，边是用来连接两个节点的，因此边至少包含两个属性一个是起始节点，另一个是终止节点。

边有一些性质，如下：

* 两个节点之间最多存在一条出边或者入边，即：不存在这样的两条边，它们的起始节点相同的同时，终止节点也相同
* 多条边可以有一个共同的起始节点，即：一个节点可以有很多个出边

针对第一个性质，我们可以用起始节点和终止节点来唯一标识一条边。

针对第二个性质，流程流转时，我们到底该走那一条边，进而可以到达下一个节点，或者我们该走哪些出边，进而可以到达下一批节点呢？
实践中，我们会在边上加上一个条件表达式的属性，这个表达式的求值为真，那么这个边就可以走。

例如：E2边上的条件表达式为${a>b}，如果此时a=2,b=1，那么这条出边可以走，如果a=1,b=2，那么这条出边不可以走。

注：具体来说，条件表达式是什么语法、采用什么解析器、变量如何传入、如何使用，这里不细展开来讲。

代码示意如下：

```java
public class Transition {
    public String from;
    public String to;
    public String condition;
}
```

#### 1.1.1.3 节点和边整合

到此为止，一个流程定义所要包含的元素我们已经给出了基本的定义，是时候把他们整合起来了。

流程定义本身我们用Definition来表示，相应的，按照ISM理论，它派生自Funcs类。

代码示意如下：

```java
public class Definition extends Funcs<Object> {
    public Set<Transition> transtions = new LinkedHashSet<>();
}
```

我们看到，这里并没有显示的表示所有节点的集合这个属性，其实它是保存在Funcs中的，
Funcs中有个属性：Map<String, Func<T>> id2Func ，可以存储Func，而Node又是继承自Func的，因此这里
是隐士的有所有节点集合这个属性的。


总结一下，到此为止，我们可以使用Definition来表示一个流程定义本身，使用Node来表示流程定义中的节点，
使用Transition来表示节点之间的边了，但是，这样的设计还远远不够，没有达到可实际落地应用的程度，接下来，
我们继续细化我们的设计。


### 1.1.2 流程节点

按照理论，流程中一共有以下几大类节点，分别是：

* 开始节点(Start)，流程以此开始流转
* 结束节点(End)，流程到此结束流转
* 子流程节点(Sub)，流程到此开始执行另外一个流程x，x执行完毕后，接着往下走
* 并行节点(Fork)，流程到此会分裂为多条并行的执行流，共同执行，直达碰到同步节点
* 同步节点(Join)，流程到此会把多条并行的执行流，汇聚为一条执行流
* 选择节点(XOR)，N选一，多条出边选择一条继续往下流转
* 发送消息节点(Tx)，发送消息给监听消息节点，然后继续往下走
* 监听消息节点(Rx)，流程到此会等待，直到指定的消息出现，才会继续往下走
* 活动节点(Activity)，通常意义上的一个节点，一个出边，一个入边

它们的关系为：

![图](http://assets.processon.com/chart_image/61cd71b0e0b34d1be77c9744.png?)


为什么这么设计呢？我们接下来逐个分析。

#### 1.1.2.1 并行节点(Fork)和同步节点(Join)

Fork和Join一定要成对的出现，即：一个Join的存在一定是为了同步某个Fork的。鉴于二者的特殊关系，我们在这一节，一并进行说明。

并行节点(Fork)和同步节点(Join)可以说是工作流中最难设计的节点之一，为了让大家能够轻松的理解设计思想和设计意图，我们一步一步的对设计进行演化，最终达到一个可用的版本，
虽然这样做有些啰嗦，也有浪费篇幅的嫌疑，但是效果不错，总体而言，利大于弊。

我们知道，流程运行到Fork节点后，一条执行流变为多条执行流，这里我们假设所有的出边都可并行流出，也就是说，一条执行流变为N条执行流，N等于Fork的出边条数。

那么如何变为多条执行流呢？根据ISM理论和模型，我们只要把下一次需要调度的多个节点放入到State的tobeStarted集合中即可，这样，当下次调度发生的时候，tobeStarted集合中的节点就会依次的被调用，从而达到多条流都被执行的目的。


Fork代码示意如下：

```java

public class Fork extends Node {

    public Fork(String id) {
        super(id);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Object> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Object> nextState) {
        outGoings.forEach(transition -> {
            nextState.tobeStarted.add(new FuncDescriptor(transition.to));
        });
    }

}


```


流程运行到Join节点后，需要同步多条执行流，即：如果多条执行流都到达了，那么流程接着往下走，否则，等待，直到它们都到达。

清楚了这个语义之后，设计时，需要Join知道对应Fork的出边条数，然后，每次被调度时，都要检查是否它们都到达了，这里使用了一个简单的计数逻辑。


Join代码示意如下：

```java

public class Join extends Node {

    public Join(String id, int count) {
        super(id);
        //记录对应Fork的出边条数
        options.put("ForkOutGoingsCount", count);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Object> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Object> nextState) {
        //每次减一
        Integer count = (Integer) options.get("ForkOutGoingsCount");
        count--;

        //减到零，意味着同步完成，流程接着往下走
        if (count == 0)
            nextState.tobeStarted.add(new FuncDescriptor(outGoings.iterator().next().to));
        else
            options.put("ForkOutGoingsCount", count);
    }

}

```


到此，有些同学可能会有疑问，那就是，这样做是伪并行的，因为ISM的调度是单线程的，节点的执行逻辑也在调度线程之内，因此，实际上还是单条执行流的。
实际上，这里有个概念上的小误解，即：多条执行流不等于多线程，它们两个不是一个概念，多线程更多的是计算机上的一个术语，强调的是同一时刻有多个逻辑被同时处理，注重于微观，
而多条执行流更多的是业务上的一个术语，强调的的是有多个事在做，注重于中观、宏观，比如：一个线程在下载，一个线程在听歌，可以说是多线程的，一个学生既要学习数学也要学习英语，可以说是多条执行流。总体来说，多条执行流对于时限上的要求没有多线程那么高。

那么，Fork-Join模型可不可以用于处理那种多线程的场景呢？答案是可以的，我们只要覆写ISM的调度逻辑，把这种Fork产生出来的任务，以线程池的方式运行并调起即可，这里就不在详细展开了。

上述设计应对非常幼稚的使用场景还是可以的，稍微复杂一点的场景，就不在适用了，举例如下。

例一、Fork的出边数不等于执行流数，如下图所示

![图](http://assets.processon.com/chart_image/61ce77745653bb069feb983d.png)

Fork有两条出边，E1、E2，这两条边上都是由条件表达式的，如果此时a=100,那么Fork会产生两条并行执行流，即：顺着E1、E2往下走，A、B被调起；
如果此时a=2,那么Fork不会产生并行执行流，而还是按照原来的那条主执行流执行，即：顺着E1往下走，A被调起；
如果此时a=0,那么Fork不会产生并行执行流，而是终止运行，因为两条出边上的条件都为False。

这就要求我们能够动态的在运行期得知并行执行流数目，而不是在流程定义时得知。

例二、有些执行流会在中途被取消

参加上图，如果a=100,那么接下A、B会被调起，由于某些原因，B被取消了，那么意味着B不会流转到Join，
在这种情况下，Join就会一直等下去，流程永远不会往下走。

这就又要求我们能够动态感知并行执行流数目的变化，从而进行更进一步的处理。

例三、动态增减出边

Fork由上图的两条出边E1、E2，在运行期增加了一条出边E3，如下图所示：

![图](http://assets.processon.com/chart_image/61ceae3e7d9c083657b0d738.png)

如果不加处理的化，那么Join将被击穿，即：Join之后的节点会被调用不止一次。

这就又要求我们能够动态修改并行执行流数，进行更进一步的处理。

为了能够解决上面这些问题，我们引入了ForkToken的概念。

一个Fork会产生很多个Token，这些Token会顺着流程在节点之间进行流转，当流转到Join节点时，如果对应的Fork的所有Token都已经拿到，意味着同步结束，流程可以接着往下走了，否则，同步等待，举例如下图所示：

![图](http://assets.processon.com/chart_image/61ceb0ed07912973efb04154.png)

当流程流转到Fork时，Fork会根据当时的条件(a=9)，产生两个token，t1、t2，即：流程接下来流转到节点A、B。
此时，A节点持有t1，B节点持有t2,如下图所示：

![图](http://assets.processon.com/chart_image/61ceb158f346fb0692ae057d.png)

接下来，A执行，A执行完毕后，流转到Join进行执行，此时Join持有t1，Join发现其并没有完全持有所有的token，故在这里等待，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb19be401fd7a538edda3.png)


接下来，B执行，B执行完毕后，流转到Join进行执行，此时Join持有t1、t2，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb1b9e401fd7a538ede43.png)

然后，Join发现其完全持有所有的token，故接着往下走，调起C，同时清空所有其对应的Fork的所有token，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb1fa1e08530666643d61.png)

至此，一个基本的ForkToken的概念就描述完毕了。细心的同学可能注意到一句话：“清空所有其对应的Fork的所有token”，为什么要这么做呢？这里是为了应对Fork-Join嵌套的情况，举个复杂的例子进行说明，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb5f60e3e7441570b2bc1.png)

Fork_A产生了三个它自己的token，a1、a2、a3，然后流程接着往下走，把它们分别传递给了接下来的三个节点，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb7f5e0b34d1be77f07e2.png)


接下来，Fork_B被调起，它产生了两个它自己的token，b1、b2，此时，Fork_B节点持有三个token，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb89707912973efb0629e.png)


接下来，Fork_B把**非自己产生的token同时传递给了A、B**，把**自己产生的token分别传递给了A、B**，如下图所示：

![图](http://assets.processon.com/chart_image/61ceb9727d9c083657b107b0.png)



接下来，某个时间点，Join_B会持有的token如下图所示：

![图](http://assets.processon.com/chart_image/61cebaaa1efad4259cef9e94.png)

此时，Join_B除了持有对应的Fork_B产生的所有token外，还持有Fork_A的一个token ，按照上面的逻辑，Join_B会清空所有Fork_B产生的token，即：b1、b2。同时把其他token接着往下传递，如下图所示：

![图](http://assets.processon.com/chart_image/61cebc84e401fd7a538f0a0d.png)

然后，按照正常逻辑，继续往下流转，对于Fork_A-Join_A来讲，Fork_B-Join_B这个嵌套好像没有发生过。

首先，恭喜您，能看到这里已经很不容易了，接下来，介绍如何用代码来实现这个设计。

在代码中，我们需要设计State中的payload具体类型，同时也需要在payload中存储token，示意代码如下：


```java

public class Data extends HashMap<String, Object> implements Map<String, Object> {

    private static final String MAP_KEY_FORKTOKENTS_MAP = "____MAP_KEY_FORKTOKENTS_MAP____";

    public Data() {
    }

    public Map<String, Set<ForkToken>> getForkTokensMap() {
        return (Map<String, Set<ForkToken>>) super.computeIfAbsent(MAP_KEY_FORKTOKENTS_MAP, k -> new HashMap<String, HashSet<ForkToken>>());
    }
}


```

节点、边、流程定义等类，也需要进行相应的修改，如下：

```java

public class Definition extends Funcs<Data> {
    public Set<Transition> transtions = new LinkedHashSet<>();
}

public abstract class Node extends Func<Data> {

    //出边集合
    public Set<Transition> outGoings = new LinkedHashSet<>();

    //入边集合
    public Set<Transition> inComings = new LinkedHashSet<>();

    //存储节点配置信息
    protected Map<String, Object> options = new HashMap<>();

    public Node(String id) {
        super(id);
    }
}

public class Transition {
    public String from;
    public String to;

    public String condition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition that = (Transition) o;
        return Objects.equals(from, that.from) &&
                Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}


```


token需要用一个类型来表示，同时，需要防重，如下：

```java

public class ForkToken {
    public String id;
    //对应的fork
    public String forkID;
    //对应的fork一共产生了多少个token
    public int total;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForkToken forkToken = (ForkToken) o;
        return total == forkToken.total &&
                Objects.equals(id, forkToken.id) &&
                Objects.equals(forkID, forkToken.forkID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, forkID, total);
    }
}

```


我们用ForkTokenManager来管理token，代码量比较多，请大家仔细阅读，如下：

```java

public class ForkTokenManager {

    public Map<String, Set<ForkToken>> nodeID2Tokens = new HashMap<>();


    public static synchronized ForkTokenManager instance(State<Data> lasted) {
        ForkTokenManager forkTokenManager = new ForkTokenManager();
        if (lasted != null) {
            Data data = lasted.payload;
            forkTokenManager.nodeID2Tokens = data.getForkTokensMap();
        }

        return forkTokenManager;
    }

    /**
     *
     * @param fromNodeID
     * @param toNodeID
     * 把fromNodeID节点持有的所有token，移动到toNodeID节点
     */
    public void pass2Node(String fromNodeID, String toNodeID) {
        Set<ForkToken> forkTokensFrom = nodeTokens(fromNodeID);
        Set<ForkToken> forkTokensTo = nodeTokens(toNodeID);
        forkTokensTo.addAll(forkTokensFrom);
        forkTokensFrom.clear();
    }

    /**
     *
     * @param forkID
     * @param nodeID
     * @return 是否nodeID节点持有所有forkID产生的token
     */
    public boolean allInOneNode(String forkID, String nodeID) {
        Set<ForkToken> nodeAllForkTokenList = nodeTokensOfFork(nodeID, forkID);
        Set<ForkToken> forkTokens = tokensOfFork(forkID);
        return nodeAllForkTokenList.size() == forkTokens.size();
    }

    private void removeToken(Set<ForkToken> tokens) {
        nodeID2Tokens.values().forEach(x -> x.removeAll(tokens));
    }

    /**
     * @param forkID
     * 清空某个forkID产生的所有token
     */
    public void removeToken(String forkID) {
        Set<ForkToken> forkTokenList = tokensOfFork(forkID);
        removeToken(forkTokenList);
    }

    //add a fork token to a holder fo nodeID

    /**
     * 添加一个token到nodeID，使得nodeID新持有一个tokenID
     * @param nodeID
     * @param forkID
     * @param tokenID
     * @return
     */
    public boolean addToken(String nodeID, String forkID, String tokenID) {
        Set<ForkToken> forkTokenList = tokensOfFork(forkID);

        //不能重复添加
        if (forkTokenList.stream().anyMatch(forkToken -> forkToken.id.equals(tokenID))) return false;

        forkTokenList.forEach(forkToken -> {
            assert forkToken.total == forkTokenList.size();
            forkToken.total++;
        });

        ForkToken forkToken = new ForkToken();
        forkToken.total = forkTokenList.size() == 0 ? 1 : forkTokenList.iterator().next().total;
        forkToken.forkID = forkID;
        forkToken.id = tokenID;

        nodeTokens(nodeID).add(forkToken);
        return true;
    }


    /**
     *
     * @param forkID
     * @return forkID产生的所有token集合
     */
    public Set<ForkToken> tokensOfFork(String forkID) {
        return this.nodeID2Tokens.values().stream().flatMap(Collection::stream)
                .filter(forkToken -> forkToken.forkID.equals(forkID))
                .collect(Collectors.toSet());
    }

    /**
     *
     * @param nodeID 节点标识
     * @return 该节点持有的所有token
     */
    public Set<ForkToken> nodeTokens(String nodeID) {
        return this.nodeID2Tokens.computeIfAbsent(nodeID, x -> new HashSet<>());
    }


    /**
     * @param nodeID
     * @param forkID
     * @return nodeID上持有的某个forkID的token集合
     */
    public Set<ForkToken> nodeTokensOfFork(String nodeID, String forkID) {
        return nodeTokens(nodeID).stream().filter(x -> x.forkID.equals(forkID)).collect(Collectors.toSet());
    }

}

```

经过上述代码铺垫，最终的Fork节点的代码来了，示意如下：

```java

public class Fork extends Node {


    //条件表达式求值接口
    private IExprEval exprEval;

    public Fork(String id, IExprEval exprEval) {
        super(id);
        this.exprEval = exprEval;
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Data> nextState) {
        ForkTokenManager forkTokenManager = ForkTokenManager.instance(nextState);

        //得到所有边上表达式为true的出边
        List<Transition> trueOutGoingTransitions = outGoings.stream()
                .filter(transition -> exprEval.eval(transition.condition))
                .collect(Collectors.toList());

        //没有合适的出边，则报错
        if (trueOutGoingTransitions.size() == 0)
            throw new RuntimeException("all outGoings are false!!");


        //得到本节点上所有的token
        Set<ForkToken> nodeTokens = forkTokenManager.nodeTokens(id);

        trueOutGoingTransitions
                .forEach(transition -> {
                    //把出边对应的终止节点加入到调度
                    nextState.tobeStarted.add(new FuncDescriptor(transition.to));
                    //传递本fork产生的token
                    forkTokenManager.addToken(transition.to, id, transition.from + "_" + transition.to);
                    //传递非本fork产生的token
                    forkTokenManager.nodeTokens(transition.to).addAll(nodeTokens);
                });

        //清空本节点持有的token
        forkTokenManager.nodeTokens(id).clear();
    }

}

```

请大家仔细阅读上述代码，加深对设计的理解。

与其配套的Join节点的代码如下：

```java

public class Join extends Node {

    public static final String KEY_FORK_ID="KEY_FORK_ID";

    public Join(String id, Map<String, Object> options) {
        super(id);
        this.options.putAll(options);
    }

    @Override
    public void eventHandle(FuncDescriptor funcDescriptor, State<Data> nextState) {

    }

    @Override
    public void transfer(FuncDescriptor funcDescriptor, State<Data> nextState) {
        Transition next = next(nextState);
        if (next != null)
            nextState.tobeStarted.add(new FuncDescriptor(next.to));
    }


    protected Transition next(State<Data> returnState) {
        ForkTokenManager forkTokenManager = ForkTokenManager.instance(returnState);

        String forkID = (String) options.get(KEY_FORK_ID);

        //所有的token都持有
        if (!forkTokenManager.allInOneNode(forkID, id)) return null;

        //清空对应的fork的所有token
        //清空后，本节点就不在持有对应的fork的任何token
        forkTokenManager.removeToken(forkID);

        Transition next = outGoings.iterator().next();

        //传递到下游
        forkTokenManager.pass2Node(id, next.to);

        return next;
    }
}

```

需要注意的是，当生成Join对象时，需要把其对应的Fork的一些信息通过options的形式传递进去，否则无法使用！！！







#### 1.1.3 流程执行



### 1.2 主要实现

有了上面的设计，我们来看看主要的实现，在代码中，如何落地上述设计。

**注：这里只是示例性代码，十分不建议直接用于生产环境。**


#### 1.2.1 状态



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

这一章介绍如何设计一个实用的工作流引擎执行服务，并给出主要


总结一下，这一章开始主要介绍了ISM的设计，采用了迭代的思想，逐步的把设计演化到了最终的版本，接下来，用代码实现了该设计的主要内容，最后，用了三个测试用例演示了如何使用代码。我相信，经过以上三步，同学们已经能够理解了ISM的原理，并且可以在生产环境中进行灵活的运用了。