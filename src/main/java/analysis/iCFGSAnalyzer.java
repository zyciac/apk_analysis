package analysis;/*
 * Generating iCFG from a chosen function
 */

import fj.P;
import org.apache.commons.io.FilenameUtils;
import polyglot.ast.Do;
import ppg.parse.ParseTest;
import soot.*;
import soot.util.Chain;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.EmptyChain;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import static soot.util.dot.DotGraph.DOT_EXTENSION;

import visual.AndroidCallGraphFilter;
import android.AndroidUtil;

public class iCFGSAnalyzer {

    public static void main(String[] args) {
        assert args.length >= 3;
        File file = new File(args[0]);
        String apkPath = file.getAbsolutePath();
        System.out.println(apkPath);
        String platformPath = args[1];
        Pattern searchPattern = Pattern.compile(Pattern.quote(args[2]), Pattern.CASE_INSENSITIVE);

        buildCallGraph(apkPath, platformPath);
//        testFunction(apkPath);
        CallGraph myCallGraph = Scene.v().getCallGraph();
        getMethodSubGraph(apkPath, myCallGraph, searchPattern);

//        if(dot==null){
//            return;
//        }
//        String destination = "./sootOutput/" + "test";
//        dot.plot(destination+DOT_EXTENSION);
    }
    public static void testFunction(String apkPath){
        String fileName = FilenameUtils.getBaseName(apkPath) + "classes.log";
        File resultFile = new File("./sootOutput/" + fileName);
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("Classes begins==================");

        Chain<SootClass> mySootClasses =  Scene.v().getClasses();
        Chain<SootClass> myLibClasses = Scene.v().getLibraryClasses();
        Chain<SootClass> myAppClasses = Scene.v().getApplicationClasses();
        System.out.println("====================");
        System.out.println(Scene.v().getEntryPoints());
        System.out.println("====================");
        System.out.println(mySootClasses.size());
        System.out.println("Lib====================");
        System.out.println(myLibClasses.size());
        System.out.println("Myapp====================");
        System.out.println(myAppClasses.size());

        out.println("classes from libclasses starting ===============");
        for (SootClass c : myLibClasses){
//            if (!mySootClasses.contains(c)){
//                System.out.println("This Lib class "+ c.toString() + "is not in SootClasses");
//            }
            out.println(c.toString());
            for (SootMethod m : c.getMethods()){
                out.println("\t "+ m.toString());
            }
        }

        out.println("classes from Appclass starting ===============");
        for (SootClass c : myAppClasses){
//            System.out.println("Hi2");
//            if (!mySootClasses.contains(c)){
//                System.out.println("This App class "+ c.toString() + "is not in SootClasses");
//            }
            out.println(c.toString());
            for (SootMethod m : c.getMethods()){
                out.println("\t "+ m.toString());
            }
        }

        out.println("classes from Soot class starting ===============");
        for (SootClass c : mySootClasses){
            out.println(c.toString());
            for (SootMethod m : c.getMethods()){
                out.println("\t "+ m.toString());
            }
        }
//        for (SootClass c : myAppClasses){
//            System.out.println("Hi3");
//            if (myLibClasses.contains(c)){
//                System.out.println("This App class "+ c.toString() + "is in the libclass");
//            }
//        }
        out.println("classes ends==================");
        out.close();
    }

    public static void analyzeDirectory(String dirName, Pattern searchPattern){
        File folder = new File(dirName);
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

        String PLATFORM_PATH = "C:\\Users\\Yichi\\Library\\Android\\sdk\\platforms";


        for (String apkPath : apkList){
            buildCallGraph(apkPath, PLATFORM_PATH);
            CallGraph cg = Scene.v().getCallGraph();
            getMethodSubGraph(apkPath, cg, searchPattern);
        }
    }
    public static void getMethodSubGraph(String apkPath, CallGraph cg, Pattern searchPattern){
        Boolean DEBUG = false;
        String apkBaseName = FilenameUtils.getBaseName(apkPath);
        QueueReader<Edge> edges = cg.listener();
        Collection<SootMethod> iMethodsSet = new HashSet<>();

        String fileName = apkBaseName+"_edge.log";
        File resultFile = new File("./sootOutput/"+fileName);
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;

        String path = "./sootOutput/" + apkBaseName;
        File directory = new File(path);
        if (!directory.exists()){
            directory.mkdir();
        }else{
            System.out.println("Deleting "+directory);
            deleteDirectory(directory);
            directory.mkdir();
        }

        if(DEBUG) {
            System.out.println("Starting searching for pattern...");
        }
        while(edges.hasNext()){
            Edge edge = edges.next();
            SootMethod target = (SootMethod) edge.getTgt();
            SootMethod src = (SootMethod) edge.getSrc();
            if(DEBUG){
                out.println(src.toString()+"->"+target.toString());
            }
            if (searchPattern.matcher(target.toString()).find()){
                if (!iMethodsSet.contains(target)){
                    iMethodsSet.add(target);
                }
//                interestMethod = target;
//                break;
            } else if (searchPattern.matcher(src.toString()).find()){
                if (!iMethodsSet.contains(src)){
                    iMethodsSet.add(src);
                }
//                interestMethod = src;
//                break;
            }
        }

        out.close();

        if(DEBUG){
            System.out.println("Search Complete: interesting Methods: " + iMethodsSet.size());
        }
        if(iMethodsSet.size() == 0){
            System.out.println("Interestings Method None, abort.");
            return;
        }
//        if(DEBUG) {
//            System.out.println("Search complete");
//            if (interestMethod == null) {
//                System.out.println("Did not found related method");
//                return null;
//            }
//        }

        Iterator<SootMethod> iMethodIter = iMethodsSet.iterator();
        while(iMethodIter.hasNext()) {
            Set<String> visited = new HashSet<>();
            SootMethod interestMethod = iMethodIter.next();
            DotGraph dot = new DotGraph("subcallgraph of method" + interestMethod.toString());

            if (DEBUG) {
                System.out.println("Starting creating subgraph of method: " + interestMethod.toString());
            }

            int maxInDepth = 3;
            int maxOutDepth = 3;
            int curInDepth = 0;
            int curOutDepth = 0;

            dot.drawNode(interestMethod.toString());
            visited.add(interestMethod.toString());

            Collection<SootMethod> inMethodsQueue = new HashSet<SootMethod>();
            Collection<SootMethod> outMethodsQueue = new HashSet<SootMethod>();

            inMethodsQueue.add(interestMethod);
            outMethodsQueue.add(interestMethod);
            //Queue<MethodOrMethodContext> inMethodQueue = getIntoMethods(interestMethod, cg);
            //        Queue<MethodOrMethodContext> outMethodQueue = getOutofMethods(interestMethod, cg);
            //        Iterator<Edge> inIter = cg.edgesInto(interestMethod);
            //        Iterator<Edge> outIter = cg.edgesInto(interestMethod);
            //
            //        while(inIter.hasNext()){
            //            MethodOrMethodContext m = inIter.next().getSrc();
            //            if (!visited.contains(m.toString())) {
            //                visited.add(m.toString());
            //                inMethodsList.add(inIter.next().getSrc());
            //            }
            //        }
            if (DEBUG) {
                System.out.println("Updating Subgraph: updating intoMethod Subgraph");
            }
            while (curInDepth < maxInDepth) {
                if (DEBUG) {
                    System.out.println("currentInDepth: " + curInDepth +
                            " current InMethodQueue has " + inMethodsQueue.size() + " element(s)");
                }
                Collection<SootMethod> tmpinMethodsQueue = new HashSet<SootMethod>();

                Iterator<SootMethod> mIter = inMethodsQueue.iterator();
                while (mIter.hasNext()) {
                    SootMethod tmpM = mIter.next();
                    for (SootMethod mIntoTmpM : getIntoMethods(tmpM, cg)) {
                        System.out.println("adding " + tmpM.toString() +
                                ' ' + mIntoTmpM.toString() + " with direction " + true);
                        addNewEdge(tmpM.toString(), mIntoTmpM.toString(), true, cg, visited, dot);
                        tmpinMethodsQueue.add(mIntoTmpM);
                    }
                }
                inMethodsQueue = tmpinMethodsQueue;
                tmpinMethodsQueue.clear();
                curInDepth += 1;
                //            while(inMethodsQueue.size()>0){
                //                MethodOrMethodContext tmpM =  inMethodsQueue.poll();
                //                if (!visited.contains(tmpM.toString()){
                //                    visited.add(tmpM.toString());
                //                    dot = updateSubGraph()
                //                }
                //            }
                if (inMethodsQueue.size() == 0) {
                    if (DEBUG) {
                        System.out.println("no more method in queue, stopping at InDepth " + curInDepth);
                    }
                    break;
                }
            }

            if (DEBUG) {
                System.out.println("Updating Subgraph: updating intoMethod Subgraph");
            }
            while (curOutDepth < maxOutDepth) {
                if (DEBUG) {
                    System.out.println("currentOutDepth: " + curOutDepth +
                            " current OutMethodQueue has " + outMethodsQueue.size() + " element(s)");
                }

                Collection<SootMethod> tmpOutMethodsQueue = new HashSet<SootMethod>();

                Iterator<SootMethod> mIter = outMethodsQueue.iterator();
                while (mIter.hasNext()) {
                    SootMethod tmpM = mIter.next();
                    for (SootMethod mOutTmpM : getOutofMethods(tmpM, cg)) {
                        //TODO
                        System.out.println("adding " + tmpM.toString() +
                                ' ' + mOutTmpM.toString() + " with direction " + false);
                        addNewEdge(tmpM.toString(), mOutTmpM.toString(), false, cg, visited, dot);
                        tmpOutMethodsQueue.add(mOutTmpM);
                    }
                }
                outMethodsQueue = tmpOutMethodsQueue;

                tmpOutMethodsQueue.clear();
                curOutDepth += 1;
                //            while(inMethodsQueue.size()>0){
                //                MethodOrMethodContext tmpM =  inMethodsQueue.poll();
                //                if (!visited.contains(tmpM.toString()){
                //                    visited.add(tmpM.toString());
                //                    dot = updateSubGraph()
                //                }
                //            }
                if (outMethodsQueue.size() == 0) {
                    if (DEBUG) {
                        System.out.println("no more method in queue, stopping at outDepth " + curOutDepth);
                    }
                    break;
                }
            }


//            String dotName = interestMethod.toString();
//            String methodClassName = dotFullName.split("")[0];
//            System.out.println(methodClassName);
            String baseName = interestMethod.getName();
            String destination = path +"/"+baseName;
            File dotFile = new File(destination+DOT_EXTENSION);
            int counter = 1;
            while (dotFile.exists()){
                String newName = baseName + Integer.toString(counter);
                destination = path + "/" + newName;
                counter += 1;
                if(DEBUG){
                    System.out.println("found existing file, changing filename to "+newName);
                }

                dotFile = new File(destination+DOT_EXTENSION);
            }
            if(DEBUG){
                System.out.println("Creating .dot file at "+destination);
            }

//            System.out.println("xxxxxxxxxxxxxxxxxxxxxxx");
            dot.plot(destination+DOT_EXTENSION);
            createPNG(destination);
//            break;
        }

    }

    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static void createPNG(String destination){

        File f = new File(destination+DOT_EXTENSION);
        String arg1 = f.getAbsolutePath();
        String arg2 = arg1+"png";
        System.out.println(arg1);
        String[] c = {"dot", "-Tpng", arg1, "-O"};
        try{
            Process p  = Runtime.getRuntime().exec(c);
            int err = p.waitFor();
        }
        catch(IOException e1){
            System.out.println(e1);
        }
        catch(InterruptedException e2){
            System.out.println(e2);
        }
    }
    /**
     *
     * @param curMethod: String
     * @param newMethod: String
     * @param direction: true for intoMethods, false for outofmethods
     * @param cg: CallGraph,
     * @param visited: Set<String>, mark if the node is visited before
     * @param dot: dot graph
     */

    public static void addNewEdge(String curMethod,
                             String newMethod,
                             Boolean direction,
                             CallGraph cg,
                             Set<String> visited,
                             DotGraph dot){

//        if (visited.contains(newMethod)){
//            return;
//        }

        visited.add(newMethod);
        dot.drawNode(newMethod);
        if (direction){
            dot.drawEdge(newMethod, curMethod);
        } else{
            dot.drawEdge(curMethod, newMethod);
        }

    }

    public static DotGraph updateSubGraph(String curMethod,
                                          String newMethod,
                                          DotGraph dot){
        dot.drawNode(newMethod.toString());
        dot.drawEdge(curMethod.toString(), newMethod.toString());
        return dot;
    }

    public static Collection<SootMethod> getIntoMethods(SootMethod func, CallGraph cg){
        Iterator<Edge> inIter = cg.edgesInto(func);
//        while(inIter.hasNext()){
//            System.out.println(func.toString() + " has intoMethods" + inIter.next().getSrc().toString());
//        }
        Collection<SootMethod> inMethodQueue = new HashSet<SootMethod>();
        while(inIter.hasNext()){
            SootMethod m = (SootMethod) inIter.next().getSrc();
            inMethodQueue.add(m);
        }
        return inMethodQueue;
    }


    public static Collection<SootMethod> getOutofMethods(SootMethod func, CallGraph cg){
        Iterator<Edge> outIter = cg.edgesOutOf(func);
        Collection<SootMethod> outMethodQueue = new HashSet<SootMethod>();
        while(outIter.hasNext()){
            SootMethod m = (SootMethod) outIter.next().getSrc();
            outMethodQueue.add(m);
        }
        return outMethodQueue;
    }

    public static void buildCallGraph(String apkDir, String platformDir) {
        SetupApplication app = new SetupApplication(platformDir, apkDir);
        long start = System.nanoTime();
        app.constructCallgraph();
        long elapsedTime = java.lang.System.nanoTime() - start;
        System.out.println(elapsedTime/1000000000 + " seconds for constructing the call graph");
    }
}
