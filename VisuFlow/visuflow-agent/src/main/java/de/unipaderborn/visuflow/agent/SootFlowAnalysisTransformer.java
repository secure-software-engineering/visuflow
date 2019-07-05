package de.unipaderborn.visuflow.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class SootFlowAnalysisTransformer implements ClassFileTransformer {

	private ClassPool pool;

	public SootFlowAnalysisTransformer() {
		pool = ClassPool.getDefault();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {

		// don't transform analyses in the soot framework
		if(className.startsWith("soot/")) {
			return null;
		}

		try {
			pool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
			CtClass cclass = pool.get(className.replaceAll("/", "."));
			CtClass superClass = cclass;
			do {
				superClass = superClass.getSuperclass();
				if (superClass == null) {
					break;
				}

				if (superClass.getName().equals("soot.toolkits.scalar.FlowAnalysis")) {
					enhance(cclass);
					if (!cclass.isFrozen()) {
						return cclass.toBytecode();
					} else {
						throw new RuntimeException(className + " is frozen");
					}
				}
			} while (superClass != null);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		// no transformation
		return null;
	}

	private void enhance(CtClass c) throws NotFoundException, CannotCompileException {
		CtMethod flowThrough = c.getDeclaredMethod("flowThrough");
		CtClass string = pool.get("java.lang.String");
		flowThrough.addLocalVariable("fqn", string);
		flowThrough.addLocalVariable("in", string);
		flowThrough.addLocalVariable("out", string);
		flowThrough.insertAfter("fqn= new String($2.getTag(\"Fully Qualified Name\").getValue());");
		flowThrough.insertAfter("in = $1.toString();");
		flowThrough.insertAfter("out = $3.toString();");
		flowThrough.insertAfter("de.unipaderborn.visuflow.agent.MonitorClient.getInstance().sendAsync(fqn, in, out);");
	}
}
