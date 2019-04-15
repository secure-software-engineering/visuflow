package de.userstudy.analysis1;

import java.io.File;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Transform;

public class Runner {

	public static void main(String[] args) {
		runAnalysis(new Reporter(), "");
	}

	public static void runAnalysis(final Reporter reporter, final String targetMethod) {
		G.reset();

		// Register the transform
		Transform transform = new Transform("jtp.analysis", new BodyTransformer() {

			@Override
			protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
				if (targetMethod == null || targetMethod.isEmpty()
						|| b.getMethod().getSignature().equals(targetMethod)) {
					Analysis ipa = new Analysis(b, reporter);
					ipa.doAnalyis();
				}
			}

		});
		PackManager.v().getPack("jtp").add(transform);

		// Run Soot
		Main.main(new String[] { "-pp", "-w", "-keep-line-number", "-no-bodies-for-excluded", "-process-dir",
				".." + File.separator + "TaintProject" + File.separator + "bin", "-src-prec", "only-class", "-output-format", "none",
				"de.visuflow.userstudy2.target" });
	}

}
