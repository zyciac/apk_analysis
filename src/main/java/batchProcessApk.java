import analysis.iCFGSAnalyzer;
import soot.Scene;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class batchProcessApk {
    public static void main(String[] args){
        assert args.length == 3;
        File folder = new File(args[0]);
        List<String> apkList = new ArrayList<>();
        for (File fileEntry : folder.listFiles()) {
            String apkFullPath = fileEntry.getAbsolutePath();
            System.out.println(apkFullPath);
            if (!apkFullPath.endsWith(".apk")){
                System.out.println(apkFullPath + " is not an Apk.");
                continue;
            }
            apkList.add(fileEntry.getAbsolutePath());

        }
        Pattern searchPattern = Pattern.compile(Pattern.quote(args[2]), Pattern.CASE_INSENSITIVE);
        long start = System.nanoTime();
        int count = 0;
        for (String apkPath : apkList){
            iCFGSAnalyzer.buildCallGraph(apkPath, args[1]);
            CallGraph cg = Scene.v().getCallGraph();
            iCFGSAnalyzer.getMethodSubGraph(apkPath, cg, searchPattern);
            count ++ ;
        }
        long elapseTime = System.nanoTime() - start;
        System.out.println("Batch Process complete: "+count+
                " apks, average process time: "+elapseTime/(1000000000*count)+" seconds");

    }
}
