package de.visuflow.reporting;

import soot.SootMethod;
import soot.Unit;
import soot.tagkit.Tag;

public class EmptyReporter implements IReporter {

    @Override
    public void report(SootMethod method, Unit statement) {
        System.out.println("Found a violation in method " + method);
        System.out.println("\tSink: " + statement);
    }

    @Override
    public void report(SootMethod method, Unit source, Unit sink) {
        System.out.println("Found a flow in method " + method);
        System.out.println("\tSource: " + source + " --- " + getFqn(source));
        System.out.println("\tSink: " + sink + " --- " + getFqn(source));
    }

    private String getFqn(Unit source) {
        Tag fqn = source.getTag("Fully Qualified Name");
        if(fqn != null) {
            return new String(fqn.getValue());
        } else {
            return "FQN not available";
        }
    }
}
