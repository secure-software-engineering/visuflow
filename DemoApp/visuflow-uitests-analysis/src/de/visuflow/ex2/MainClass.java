package de.visuflow.ex2;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import de.visuflow.reporting.EmptyReporter;
import de.visuflow.reporting.IReporter;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;
import soot.util.Chain;

public class MainClass {

    public static void main(String[] args) {
        runAnalysis(new EmptyReporter(), 3);
    }

    public static void runAnalysis(final IReporter reporter, final int exercisenumber) {
        G.reset();

        // Register the transform
        Transform transform = new Transform("jtp.analysis", new BodyTransformer() {
            @Override
            protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
                IntraproceduralAnalysis ipa = new IntraproceduralAnalysis(b, reporter);
                ipa.doAnalyis();
            }
        });

        // Register the FQN tagger
        Transform fqnTagger = new Transform("wjtp.myTransform", new SceneTransformer() {
            @Override
            protected void internalTransform(String phase, Map<String, String> arg1) {
                Chain<SootClass> classes = Scene.v().getClasses();
                for (SootClass sootClass : classes) {
                    if(sootClass.isJavaLibraryClass()) {
                        continue;
                    }

                    for (SootMethod sootMethod : sootClass.getMethods()) {
                        Body body = sootMethod.retrieveActiveBody();
                        for (Unit unit : body.getUnits()) {
                            addFullyQualifiedName(unit, sootClass, sootMethod);
                        }
                    }
                }
            }

            private void addFullyQualifiedName(final Unit unit, final SootClass sootClass, final SootMethod sootMethod) {
                unit.addTag(new Tag() {
                    @Override
                    public byte[] getValue() throws AttributeValueException {
                        String fqn = sootClass.getName() + "." + sootMethod.getName() + ".§§" + unit.toString()+"§§";
                        try {
                            return fqn.getBytes("utf-8");
                        } catch (UnsupportedEncodingException e) {
                            AttributeValueException ave = new AttributeValueException();
                            ave.initCause(e);
                            throw ave;
                        }
                    }

                    @Override
                    public String getName() {
                        return "visuflow.unit.fqn";
                    }
                });
            }
        });


     // Run Soot
//      PackManager.v().getPack("wjtp").add(fqnTagger);
      PackManager.v().getPack("jtp").add(transform);
//      String targetDir = "/home/henni/devel/pg/workspace-plugin/visuflow-workspace/TaintProject/bin/";
//      Main.main(new String[] { "-pp", "-process-dir", targetDir, "-w", "-exclude", "javax", "-allow-phantom-refs", "-no-bodies-for-excluded", "-src-prec", "class", "-output-format", "none" });
  
//
//      String rtJar = System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";
//
      Main.main(new String[] { "-pp", "-exclude", "javax", "-allow-phantom-refs", "-no-bodies-for-excluded", 
    		  "-process-dir", "E:\\runtime-New_configuration\\TaintProject\\bin", "-src-prec",
              "only-class", "-output-format", "none", "de.visuflow.analyzeMe.ex2.TargetClass2" });
      
    }

}
