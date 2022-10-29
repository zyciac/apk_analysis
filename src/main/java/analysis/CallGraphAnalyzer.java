package analysis;/*
 * Generate detailed Call Graph
 */
import org.apache.commons.io.FilenameUtils;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;
import soot.Body;
import soot.Unit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.List;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import static soot.util.dot.DotGraph.DOT_EXTENSION;

import visual.AndroidCallGraphFilter;
import android.AndroidUtil;

public class CallGraphAnalyzer {
    /**
     *
     * @param args args[0]: the apk file; "apk path"
     *             args[1]: android.jar; "C:\Users\Yichi\Library\Android\sdk\platforms"
     */
    private static int icfgEdges = 0;

    public static void main(String[] args) {
        assert args.length >= 2;
        File file = new File(args[0]);
        String apkPath = file.getAbsolutePath();
        String platformPath = args[1];

        buildCallGraph(apkPath, platformPath);
        DotGraph dot = new DotGraph("callgraph");
//        analyzeCG(dot, Scene.v().getCallGraph());
//        getEntryCG(dot, Scene.v().getCallGraph(), apkPath);
        getICFG(apkPath);
        String dest = file.getName();
        String fileNameWithOutExt = FilenameUtils.removeExtension(dest);
        String destination = "./sootOutput/" + fileNameWithOutExt;
//        dot.plot(destination + DOT_EXTENSION);
        System.out.println("test");
    }

    /**
     * Iterate over the call Graph by visit edges one by one.
     * @param dot dot instance to create a dot file
     * @param cg call graph
     */
    public static void analyzeCG(DotGraph dot, CallGraph cg) {
        QueueReader<Edge> edges = cg.listener();

        Set<String> visited = new HashSet<>();
        Set<String> mainactivity = new HashSet<>();

        File resultFile = new File("./sootOutput/CG.log");
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("CG begins==================");
        // iterate over edges of the call graph
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod target = (SootMethod) edge.getTgt();
            MethodOrMethodContext src = edge.getSrc();
            if (!visited.contains(src.toString())) {
                dot.drawNode(src.toString());
                visited.add(src.toString());
            }
            if (!visited.contains(target.toString())) {
                dot.drawNode(target.toString());
                visited.add(target.toString());
            }
            out.println(src + "  -->   " + target);
            dot.drawEdge(src.toString(), target.toString());
        }

        out.println("CG ends==================");
        out.close();
        System.out.println(cg.size());
    }


    /**
     * First find all the entry points for the apps
     * Second, plot the call graph by iterating all the out-edge OR the In-edge OR
     * both the in and out edges, starting from the entry points
     * @param dot
     * @param cg
     * @param apkPath
     */
    public static void getEntryCG(DotGraph dot, CallGraph cg, String apkPath){

        QueueReader<Edge> edges = cg.listener();
        Set<String> visited = new HashSet<>();
        Set<String> resultSet = new HashSet<>();

        String fileName = FilenameUtils.getBaseName(apkPath) + "_entry.log";
        File resultFile = new File("./sootOutput/" + fileName);
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("CG begins==================");

//        int classIndex = 0;
//        AndroidCallGraphFilter androidCallGraphFilter = new AndroidCallGraphFilter(AndroidUtil.getPackageName(apkPath));
//        for(SootClass sootClass: androidCallGraphFilter.getValidClasses()){
//            System.out.println(String.format("Class %d: %s", ++classIndex, sootClass.getName()));
//            for(SootMethod sootMethod : sootClass.getMethods()){
//                int incomingEdge = 0;
//                for(Iterator<Edge> it = cg.edgesInto(sootMethod); it.hasNext(); incomingEdge++,it.next());
//                int outgoingEdge = 0;
//                for(Iterator<Edge> it = cg.edgesOutOf(sootMethod); it.hasNext();outgoingEdge++,it.next());
//                System.out.println(String.format("\tMethod %s, #IncomeEdges: %d, #OutgoingEdges: %d", sootMethod.getName(), incomingEdge, outgoingEdge));
//            }
//        }
//        System.out.println("-----------");


        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod target = (SootMethod) edge.getTgt();
            MethodOrMethodContext src = edge.getSrc();
            String searchPattern = "mainactivity";
            Boolean isSearchResult = Pattern.compile(Pattern.quote(searchPattern), Pattern.CASE_INSENSITIVE).matcher(src.toString()).find();

            if (!visited.contains(src.toString())) {
//                dot.drawNode(src.toString());
                visited.add(src.toString());

            }

            if (!visited.contains(target.toString())) {
//                dot.drawNode(target.toString());
                visited.add(target.toString());
            }
            out.println(src + "  -->   " + target);
//            dot.drawEdge(src.toString(), target.toString());
        }
        out.println("CG ends==================");
        out.close();
//        System.out.println(cg.size());
    }

    /**
     * Get the icfgEdges from the app
     */
    public static int getICFG(String apkpath){
//        if (icfgEdges > 0){
//            return icfgEdges;
//        }

        /*creating output file*/
        String fileName = FilenameUtils.getBaseName(apkpath)+"_reachable_method_names.txt";
        File outFile = new File("./sootOutput/" + fileName);
        PrintWriter out = null;
        try {
            out = new PrintWriter(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        System.out.println("The scene.v() is of class " + Scene.v().getClass());
        System.out.println("it has methods: " + Scene.v().getClass().getDeclaredMethods());
        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
        while (listener.hasNext()) {
            MethodOrMethodContext next = listener.next();
            SootMethod method = next.method();
//            System.out.println(Scene.v().getMethod(method.toString()));
            if (method.hasActiveBody()) {
                out.print("active body: \t");
            }
            else{
                out.print("no active body\t");
            }
            out.println(method.toString());
            if (!method.hasActiveBody()){
                continue;
            }


            Body activeBody = method.getActiveBody();
//            TODO what is getUnits
            System.out.println(method.toString());
            for (Unit u : activeBody.getUnits()) {
                List<Unit> succsOf = icfg.getSuccsOf(u);
//                out.println(succsOf);
                for (Unit n : succsOf){
                    out.println(n.toString());
                }
                icfgEdges += succsOf.size();

                if (icfg.isCallStmt(u)) {
                    out.println("callsite:\t" + u.toString() + "calling " + icfg.getCalleesOfCallAt(u));
                    Iterator<SootMethod> iterator = icfg.getCalleesOfCallAt(u).iterator();
                    while (iterator.hasNext()) {
                        System.out.println("callee: " + iterator.next().toString());
                    }
                    icfgEdges += icfg.getCalleesOfCallAt(u).size();
                }
                if (icfg.isExitStmt(u)) {
                    icfgEdges += icfg.getCallersOf(method).size();
                }
            }
            break;
        }
        System.out.println("icfgEdges number" + icfgEdges);
        out.println("method names end ==========");
        out.close();
        return icfgEdges;
    }
    public static void buildCallGraph(String apkDir, String platformDir) {
        SetupApplication app = new SetupApplication(platformDir, apkDir);
        long start = System.nanoTime();
        app.constructCallgraph();
        long elapsedTime = java.lang.System.nanoTime() - start;
        System.out.println(elapsedTime/1000000000 + " seconds");

    }

}
