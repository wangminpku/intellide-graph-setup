package cn.edu.pku.sei.intellide.graph.qa.code_search;

import javafx.util.Pair;

import java.util.*;

public class APILocater {

    GraphReader graphReader;
    List<MyNode> graph; // 只读

    public APILocater(GraphReader reader){
        this.graphReader = reader;
        this.graph = graphReader.getAjacentGraph();
    }

    /*
     * 对于一个query词袋，首先找到每个词对应的候选结点结合，即List<Set<MyNode>> rootNodeSet
     * 从size最小的候选结点集合S开始，对于S中的每个结点，都生成一个包含所有query词的子图
     * 从这些子图中，选择最优的。目前的最优条件为子图越小越好，大小相同则结点的权重之和越大越好。
     */
    public MySubgraph query(Set<String> queryList){
        List<Set<MyNode>> rootNodeSet = new ArrayList<>();
        for (String word: queryList){
            Set<MyNode> cur = new HashSet<>();
            for (MyNode node: graph){
                for (String nodeWord:node.cnWordSet)
                    if (CnToEnDirectory.matches(word,nodeWord)) {
                        cur.add(node);
                        break;
                    }
            }
            if (cur.size() > 0) {
                //System.out.println("- " + word + "找到了rootNode");
                rootNodeSet.add(cur);
                //System.out.println(word + " node set size: " + cur.size());
            } else {
                //System.out.println(word + " has relevant no node");
            }
        }
        if (rootNodeSet.size() == 0){
            //System.out.println("no matched nodes found");
            return null;
        }
        int minSize = Integer.MAX_VALUE;
        int startSetIndex = 0;
        for (int i = 0; i < rootNodeSet.size(); ++i){ // find the smallest root set
            if (rootNodeSet.get(i).size() < minSize){
                minSize = rootNodeSet.get(i).size();
                startSetIndex = i;
            }
        }

        List<Pair<Integer,MySubgraph>> candidateList = new ArrayList<>();
        minSize = Integer.MAX_VALUE;
        Set<MyNode> startSet = rootNodeSet.get(startSetIndex);
        for (MyNode node : startSet){
            MySubgraph subgraph = BFS(node, rootNodeSet);
            if (subgraph == null)
                continue;
            candidateList.add(new Pair<>(subgraph.nodes.size(), subgraph));
            if (subgraph.nodes.size() < minSize){
                minSize = subgraph.nodes.size();
            }
        }
        if (candidateList.size() == 0) // cannot find a subgraph
            return null;

        List<MySubgraph> optimal = new ArrayList<>();
        for (Pair<Integer, MySubgraph> pair : candidateList){ // get the minSize subgraph
            if (pair.getKey() == minSize)
                optimal.add(pair.getValue());
        }
        // if size equals, consider the node weight
        optimal.sort(Comparator.comparingDouble(x->x.rootWeightSum));
        return optimal.get(optimal.size()-1);
    }

    /*
     * 从一个根节点集合中的一个结点开始，搜索一个包含所有根集的子图
     * 最多进行 rootNodeSet.size次广搜，每次广搜没有跳数限制，最坏O(n)时间
     */
    public MySubgraph BFS(MyNode start, List<Set<MyNode>> rootNodeSet){
        boolean[] coveredRoot = new boolean[rootNodeSet.size()];
        int coveredCnt = 0;
        for (int i = 0; i < coveredRoot.length; ++i){ // start node may cover several roots
            if (rootNodeSet.get(i).contains(start)){
                coveredRoot[i] = true;
                coveredCnt++;
            }
        }

        Set<MyNode> selected = new HashSet<>(); // selected nodes
        selected.add(start);
        List<Pair<MyNode, MyNode>> paths = new ArrayList<>();
        Queue<MyNode> Q = new LinkedList<>();
        Set<MyNode> visited = new HashSet<>();

        while(coveredCnt < coveredRoot.length) { // until all roots are coverd
            Q.clear();
            visited.clear();
            for (MyNode node : selected) {
                node.father = null;
                Q.offer(node);
                visited.add(node);
            }
            boolean found = false;
            while (!Q.isEmpty()) {
                MyNode head = Q.poll();
                for (int i = 0; i < coveredRoot.length; ++i) { //head may cover several roots
                    if (!coveredRoot[i] && rootNodeSet.get(i).contains(head)) { // find a new root
                        coveredRoot[i] = true;
                        coveredCnt++;
                        found = true;
                    }
                }
                if (found){
                    selected.add(head);
                    MyNode tmp = head;
                    while (tmp.father != null) { // trace the father chain to recover the path
                        paths.add(new Pair<>(tmp, tmp.father));
                        tmp = tmp.father;
                    }
                    break;
                }
                for (MyNode next : head.neighbors) {
                    if (!visited.contains(next)) {
                        visited.add(next);
                        next.father = head;
                        Q.offer(next);
                    }
                }
            }
            if (!found) { // the selected roots cannot expand to other root, fail to construct a subgraph
                return null;
            }
        }
        MySubgraph subgraph = new MySubgraph();
        for (MyNode node : selected) {
            subgraph.selectedRoot.add(node.id);
            subgraph.rootWeightSum += node.weight;
        }
        subgraph.nodes.addAll(subgraph.selectedRoot);
        for (Pair<MyNode, MyNode> pair : paths){
            long n1 = pair.getKey().id;
            long n2 = pair.getValue().id;
            subgraph.nodes.add(n1);
            subgraph.nodes.add(n2);
            long edgeId = graphReader.getEdgeIdByNodes(n1, n2);
            subgraph.edges.add(edgeId);
        }
        return subgraph;
    }
}
