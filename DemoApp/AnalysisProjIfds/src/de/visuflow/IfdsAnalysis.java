
package de.visuflow;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import heros.solver.Pair;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Local;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.toolkits.ide.JimpleIFDSSolver;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.util.Chain;

public class IfdsAnalysis {


    public static void runAnalysis() {
        setupSoot("de.visuflow.hero.TargetHero");
        Transform transform = new Transform("wjtp.ifds", new SceneTransformer() {

            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
            	 System.out.println("running in wjtp");
            	JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
                InterProceduralAnalysis problem = new InterProceduralAnalysis(icfg);
                @SuppressWarnings({ "unchecked", "rawtypes" })
				JimpleIFDSSolver<?, InterproceduralCFG<Unit, SootMethod>> solver = new JimpleIFDSSolver(problem, true);
                solver.solve();
                
                Chain<SootClass> classes = Scene.v().getClasses();
                
                for (SootClass sootClass : classes) {
        			if (sootClass.isJavaLibraryClass() || sootClass.isLibraryClass()) {
        				continue;
        			}

        			for (SootMethod sootMethod : sootClass.getMethods()) {
        				     System.out.print("method body "+sootMethod.getActiveBody().toString());
        			}
        		}
            } 
        });
        PackManager.v().getPack("wjtp").add(transform);
        
        Main.main(new String[] {"-pp", "-w", "-keep-line-number", "-no-bodies-for-excluded",
        "-process-dir", ".." + File.separator + "TargetProjHero" + File.separator + "bin",
        "-src-prec", "only-class", "-output-format", "none"}); 
    }	//"–p jj.ule enabled:false" , "-output-dir", "E:\\work\\SSE"
    
    private static void setupSoot(String targetTestClassName) {
        G.reset();
		Path path = Paths.get(".." + File.separator + "TargetProjHero" + File.separator + "bin");
        Options.v().set_soot_classpath(path.toString());

        // We want to perform a whole program, i.e. an interprocedural analysis.
        // We construct a basic CHA call graph for the program
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg.cha", "on");
        Options.v().setPhaseOption("cg", "all-reachable:true");

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
		Options.v().set_prepend_classpath(false);

        Scene.v().addBasicClass("java.lang.StringBuilder");
        SootClass c = Scene.v().forceResolve(targetTestClassName, SootClass.BODIES);
        if (c != null) {
            c.setApplicationClass();
        }
        Scene.v().loadNecessaryClasses();
    }
    
    public static void main(String[] args) {
        runAnalysis();

    }

}
