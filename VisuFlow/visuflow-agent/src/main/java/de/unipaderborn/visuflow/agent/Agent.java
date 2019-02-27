package de.unipaderborn.visuflow.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * JVM agent, which enhances the soot framework with functionality used by the visuflow plug-in.
 * It is used to:
 * <ul>
 * <li>tag units with a fully qualified to make them identifyable</li>
 * <li>open a socket connection to the plug-in to transmit flow facts</li>
 * <li>enhance the user analysis code to send the flow facts over the socket conenction</li>
 *
 * @author henni@upb.de
 */
/* TODO add transformers for
 * Soot:
 * - soot.toolkits.scalar.BranchedFlowAnalysis
 * Heros:
 * -
 */
public class Agent {

	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		inst.addTransformer(new SootMainTransformer(), true);
		inst.addTransformer(new UnitFqnTagger(), true);
		inst.addTransformer(new SootFlowAnalysisTransformer(), true);
		inst.addTransformer(new HerosIdeTarnsformer(), true);
	}
}
