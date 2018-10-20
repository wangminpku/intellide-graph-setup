package cn.edu.pku.sei.intellide.graph.extraction.code_mention_detector.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.edu.pku.sei.intellide.graph.extraction.javacode_to_neo4j.JavaCodeGraphBuilder;
import org.neo4j.graphdb.*;

public class CodeIndexes {

    public Map<String, Long> typeMap = new HashMap<>();
    private Map<Long, String> idToTypeNameMap = new HashMap<>();
    private Map<String, Set<Long>> methodMap = new HashMap<>();
    private Map<Long, String> idToMethodNameMap = new HashMap<>();
    public Map<String, Set<Long>> typeShortNameMap = new HashMap<>();
    private Map<String, Set<Long>> methodMidNameMap = new HashMap<>();
    public Map<String, Set<Long>> methodShortNameMap = new HashMap<>();

    public CodeIndexes(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Node> nodes = db.getAllNodes().iterator();
            Set<Node> codeNodes = new HashSet<>();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                if (node.hasLabel(JavaCodeGraphBuilder.CLASS) || node.hasLabel(JavaCodeGraphBuilder.METHOD)) {
                        codeNodes.add(node);
                }
            }

            for (Node codeNode : codeNodes) {
                String name = "";
                boolean type = true;
                if (codeNode.hasLabel(JavaCodeGraphBuilder.CLASS))
                    name = (String) codeNode.getProperty(JavaCodeGraphBuilder.FULLNAME);
                if (codeNode.hasLabel(JavaCodeGraphBuilder.METHOD)){
                    //System.out.println(codeNode.getProperty("fullName"));
                    //TODO:存在重复节点没有任何边关系，出现异常需解决
                    if(codeNode.hasRelationship(JavaCodeGraphBuilder.HAVE_METHOD, Direction.INCOMING)){
                        name = codeNode.getRelationships(JavaCodeGraphBuilder.HAVE_METHOD, Direction.INCOMING).iterator().next().getStartNode().getProperty(JavaCodeGraphBuilder.FULLNAME)
                                + "." + codeNode.getProperty(JavaCodeGraphBuilder.NAME);
                        type = false;
                    }

                }
                if (name.contains("$"))
                    continue;
                if (type) {
                    typeMap.put(name, codeNode.getId());
                    idToTypeNameMap.put(codeNode.getId(), name);
                    String shortName = name;
                    int p = shortName.lastIndexOf('.');
                    if (p > 0)
                        shortName = shortName.substring(p + 1, shortName.length());
                    if (!typeShortNameMap.containsKey(shortName))
                        typeShortNameMap.put(shortName, new HashSet<>());
                    typeShortNameMap.get(shortName).add(codeNode.getId());
                } else {
                    if (!methodMap.containsKey(name))
                        methodMap.put(name, new HashSet<>());
                    methodMap.get(name).add(codeNode.getId());
                    idToMethodNameMap.put(codeNode.getId(), name);
                    int p1 = name.lastIndexOf('.');
                    int p2 = name.lastIndexOf('.', p1 - 1);
                    String midName, shortName;
                    if (p2 > 0) {
                        midName = name.substring(p2 + 1);
                        shortName = name.substring(p1 + 1);
                    } else {
                        midName = name;
                        shortName = name.substring(p1 + 1);
                    }
                    if (!methodMidNameMap.containsKey(midName))
                        methodMidNameMap.put(midName, new HashSet<>());
                    methodMidNameMap.get(midName).add(codeNode.getId());
                    if (!methodShortNameMap.containsKey(shortName))
                        methodShortNameMap.put(shortName, new HashSet<>());
                    methodShortNameMap.get(shortName).add(codeNode.getId());
                }
            }

            tx.success();
        }
    }

}