package cn.edu.pku.sei.intellide.graph.qa.code_search;

import java.util.HashSet;
import java.util.Set;

public class MySubgraph {
    double rootWeightSum = 0;
    Set<Long> selectedRoot = new HashSet<>();
    Set<Long> nodes = new HashSet<>();
    Set<Long> edges = new HashSet<>();
    public void print(){
        //System.out.println("subgraph nodes size: " + nodes.size() + "\nnodes:");
        /*for (long id : nodes){
            System.out.println(id);
        }*/
    }
}
