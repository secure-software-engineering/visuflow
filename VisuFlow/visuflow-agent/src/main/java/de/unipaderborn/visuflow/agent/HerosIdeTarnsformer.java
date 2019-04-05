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

public class HerosIdeTarnsformer implements ClassFileTransformer {

	private ClassPool pool;

	public HerosIdeTarnsformer() {
		pool = ClassPool.getDefault();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {

		if(className.equals("heros/solver/IDESolver")) {
			try {
				pool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
				CtClass cclass = pool.get(className.replaceAll("/", "."));
				enhance(cclass);
				if (!cclass.isFrozen()) {
					return cclass.toBytecode();
				} else {
					throw new RuntimeException(className + " is frozen");
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}


		// no transformation
		return null;
	}

	private void enhance(CtClass c) throws NotFoundException, CannotCompileException {
		CtMethod method = c.getDeclaredMethod("processNormalFlow");
		CtClass string = pool.get("java.lang.String");
		method.addLocalVariable("fqn", string);
		method.addLocalVariable("in", string);
		method.addLocalVariable("out", string);
		method.insertAt(601, "fqn= new String(((soot.Unit)m).getTag(\"Fully Qualified Name\").getValue());" +
				"in = d2.toString();" +
				"out = res.toString();" +
				"de.unipaderborn.visuflow.agent.MonitorClient.getInstance().sendAsync(fqn, in, out);");
	}
}
