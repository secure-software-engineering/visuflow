package de.userstudy.analysis1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;

public class Reporter {

	Map<Unit, Set<Unit>> warnings = new HashMap<Unit, Set<Unit>>();

	public void report(SootMethod method, Unit source, Unit sink) {
		Set<Unit> sinksForSource = warnings.get(source);
		if (sinksForSource == null)
			sinksForSource = new HashSet<Unit>();

		if (sinksForSource.contains(sink))
			return;

		sinksForSource.add(sink);
		warnings.put(source, sinksForSource);
		System.out.println("Leak spotted in method: " + method);
		System.out.println("\tl." + source.getJavaSourceStartLineNumber() + " Source: " + source);
		System.out.println("\tl." + sink.getJavaSourceStartLineNumber() + " Sink: " + sink);
	}
}
