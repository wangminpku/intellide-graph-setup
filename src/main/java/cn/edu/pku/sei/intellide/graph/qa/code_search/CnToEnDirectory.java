package cn.edu.pku.sei.intellide.graph.qa.code_search;

import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CnToEnDirectory {

    static private Set<Pair<String,String>> directory=new HashSet<>();


    static {
        List<String> lines;
        try {
            lines= FileUtils.readLines(new File("C://config//cn2en.txt"),"utf-8");
            //lines= FileUtils.readLines(new File("/data/cn2en.txt"),"utf-8");
            for (String line:lines){
                String[] eles=line.trim().split("\\s+");
                if (eles.length<2)
                    continue;
                for (int i=0;i<eles.length;i++)
                    for (int j=i+1;j<eles.length;j++)
                        directory.add(new ImmutablePair<>(eles[i],eles[j].toLowerCase()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean matches(String word1, String word2){
        return word1.toLowerCase().equals(word2.toLowerCase())||directory.contains(new ImmutablePair<>(word1.toLowerCase(),word2.toLowerCase()))
                ||directory.contains(new ImmutablePair<>(word2.toLowerCase(),word1.toLowerCase()));
    }

}
