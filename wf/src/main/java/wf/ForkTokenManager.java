package wf;

import ism.State;

import java.util.*;
import java.util.stream.Collectors;

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
