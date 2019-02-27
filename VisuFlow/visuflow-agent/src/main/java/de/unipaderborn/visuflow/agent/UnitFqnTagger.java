package de.unipaderborn.visuflow.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;

/**
 * Enhances soot.SootMethod.setActiveBody
 * This transformer adds a FqnTag (soot.tagkit.Tag) to each unit, which contains
 * the fully qualified name of a unit to make it identifyable between JVM sessions.
 * At the moment the FQN consists of "{className}.{methodName}.{unit.toString()}"
 *
 * @author henni@upb.de
 *
 */
public class UnitFqnTagger implements ClassFileTransformer {

	private ClassPool pool;

	public UnitFqnTagger() {
		pool = ClassPool.getDefault();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {
		if (className.equals("soot/SootMethod")) {
			try {
				pool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
				CtClass cclass = pool.get(className.replaceAll("/", "."));

				// create a FqnTag class, which can be used to tag the unit with the FQN string
				CtClass fqnTag = pool.makeClass("FqnTag");
				fqnTag.setInterfaces(new CtClass[] {pool.get("soot.tagkit.Tag")});
				fqnTag.addField(new CtField(pool.get("java.lang.String"), "fqn", fqnTag));
				fqnTag.addConstructor(CtNewConstructor.make("public FqnTag(String fqn) { this.fqn = fqn; }", fqnTag));
				fqnTag.addMethod(CtNewMethod.make("public String getName() { return \"Fully Qualified Name\"; }", fqnTag));
				fqnTag.addMethod(CtNewMethod.make("public String toString() { return \"Fully Qualified Name: \" + this.fqn; }", fqnTag));
				fqnTag.addMethod(CtNewMethod.make("public byte[] getValue() throws soot.tagkit.AttributeValueException { return fqn.getBytes(); }", fqnTag));
				fqnTag.toClass();

				// add a method to add an FqnTag to a unit
				CtMethod addFqn = CtNewMethod.make("public void addFqn(soot.Unit unit) { unit.addTag(new FqnTag(getDeclaringClass().getName() + \".\" + getName() + \".\" + unit.toString())); }", cclass);
				cclass.addMethod(addFqn);

				// add while loop to the end of setActiveBody, which iterates over the units of the body and adds a FqnTag
				CtMethod method = cclass.getDeclaredMethod("setActiveBody");
				method.addLocalVariable("it", pool.get("java.util.Iterator"));
				method.insertAfter("it = $1.getUnits().iterator();");
				method.insertAfter("while(it.hasNext()) { addFqn((soot.Unit)it.next()); }");
				// TODO we could add the index (line number) in the current method to the FQN to make it really unique. At the moment
				// two units with the same String representation in the same method would have the same FQN (name conflict)


				if (!cclass.isFrozen()) {
					return cclass.toBytecode();
				} else {
					throw new RuntimeException(className + " is frozen");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		} else {
			// no transformation
			return null;
		}
	}
}
