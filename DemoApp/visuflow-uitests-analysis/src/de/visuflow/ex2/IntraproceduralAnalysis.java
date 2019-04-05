package de.visuflow.ex2;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.visuflow.reporting.IReporter;
import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.AbstractInstanceOfExpr;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class IntraproceduralAnalysis extends ForwardFlowAnalysis<Unit, Set<FlowAbstraction>> {
    public int flowThroughCount = 0;
    private final SootMethod method;
    private final IReporter reporter;

    public IntraproceduralAnalysis(Body b, IReporter reporter) {
        super(new ExceptionalUnitGraph(b));
        this.method = b.getMethod();
        this.reporter = reporter;
//        System.out.println(b);
//        System.out.println("%%%%%%%%%");
    }

    @Override
    protected void flowThrough(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        // this set collect all taints of the current iteration
        // at the end this will be merged with the taints from in set
        // and "returned" as the out set
        Set<FlowAbstraction> taintsOfThisIteration = new HashSet<FlowAbstraction>();
        // several different checks for taints
        checkGetSecret(in, d, taintsOfThisIteration);
        checkTaintOverwrite(in, d, taintsOfThisIteration);
        checkAssignmentOfTaintedLocal(in, d, taintsOfThisIteration);
        checkAssignmentOfTaintedClassField(in, d, taintsOfThisIteration);
        checkLeak(in, d, taintsOfThisIteration);
        checkTaintedOperand(in, d, taintsOfThisIteration);

        // merge previously found taints with the taints of the current iteration
        merge(in, taintsOfThisIteration, out);
        flowThroughCount++;
    }

    /**
     * Checks, if the field of a tainted class is assigned to a Local or another Field
     * @param in
     * @param d
     * @param out
     */
    private void checkAssignmentOfTaintedClassField(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if (d instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt def = (AbstractDefinitionStmt) d;
            Value leftSide = def.getLeftOp();
            Value rightSide = def.getRightOp();
            if ((leftSide instanceof Local || leftSide instanceof FieldRef) && rightSide instanceof FieldRef) {
                FieldRef ref = (FieldRef) rightSide;
                SootClass declaringClass = ref.getField().getDeclaringClass();
                if(declaringClass.hasTag(Tainted.NAME)) {
                    taint(leftSide, d, out);
                    d.addTag(declaringClass.getTag(Tainted.NAME));
                    
                    d.addTag(new Tag() {
						@Override
						public byte[] getValue() throws AttributeValueException {
							return "foobar".getBytes();
						}
						
						@Override
						public String getName() {
							return "visuflow.attrubte";
						}
					});
                    
                    
                }
            }
        }
    }

    /**
     * Wrapper for different operand checks
     * @see #checkArithmeticOperands(Set, Unit, Set)
     * @see #checkLogicOperands(Set, Unit, Set)
     * @see #checkInstanceOf(Set, Unit, Set)
     * @param in
     * @param d
     * @param out
     */
    private void checkTaintedOperand(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        checkInstanceOf(in, d, out);
        checkLogicOperands(in, d, out);
        checkArithmeticOperands(in, d, out);
    }

    /**
     * Checks, if one of the operands of an arithmetic operation is tainted
     * @param in
     * @param d
     * @param out
     */
    private void checkArithmeticOperands(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        checkBinaryOperation(in, d, out);
    }

    /**
     * Checks, if one of the operands of a logic operation is tainted
     * @param in
     * @param d
     * @param out
     */
    private void checkLogicOperands(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        checkBinaryOperation(in, d, out);
    }

    /**
     * Checks, if one of the operands of a binary operation is tainted. This method is used by
     * checkArithmeticOperands and checkLogicOperands
     * @param in
     * @param d
     * @param out
     */
    private void checkBinaryOperation(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if (d instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt def = (AbstractDefinitionStmt) d;
            Value leftSide = def.getLeftOp();
            Value rightSide = def.getRightOp();
            if(leftSide instanceof Local && rightSide instanceof AbstractBinopExpr) {
                AbstractBinopExpr expr = (AbstractBinopExpr) rightSide;
                FlowAbstraction taint1 = isInTaintedSet(expr.getOp1(), in);
                FlowAbstraction taint2 = isInTaintedSet(expr.getOp2(), in);
                if(taint1 != null || taint2 != null) {
                    taint(leftSide, d, out);
                }
            }
        }
    }

    /**
     * Checks, if the operand of an instanceof expression is tainted.
     * @param in
     * @param d
     * @param out
     */
    private void checkInstanceOf(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if (d instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt def = (AbstractDefinitionStmt) d;
            Value leftSide = def.getLeftOp();
            Value rightSide = def.getRightOp();
            if(leftSide instanceof Local && rightSide instanceof AbstractInstanceOfExpr) {
                AbstractInstanceOfExpr expr = (AbstractInstanceOfExpr) rightSide;
                FlowAbstraction taint = isInTaintedSet(expr.getOp(), in);
                if(taint != null) {
                    taint(leftSide, d, out);
                }
            }
        }
    }

   /**
     * Checks, if sensible information is leaked.
     * @param in
     * @param d
     * @param out
     */
    private void checkLeak(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        checkParameterLeak(in,d,out);
        checkReturnLeak(in,d,out);
    }

    /**
     * Checks, if a tainted item is returned from a method.
     * In case of a leak, it is reported with the IReporter.
     * @param in
     * @param d
     * @param out
     */
    private void checkReturnLeak(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if(d instanceof ReturnStmt) {
            ReturnStmt ret = (ReturnStmt) d;
            FlowAbstraction taint = isInTaintedSet(ret.getOp(), in);
            if(taint != null) {
                Unit source = taint.getSource();
                reporter.report(method, source, d);
            }
        }
    }

    /**
     * Checks, if a tainted variable gets passed as a parameter to another method.
     * In case of a leak, it is reported with the IReporter.
     * @param in
     * @param d
     * @param out
     */
    private void checkParameterLeak(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if (d instanceof Stmt) {
            Stmt stmt = (Stmt) d;
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr().getArgCount() > 0) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                for (Value arg : invokeExpr.getArgs()) {
                    FlowAbstraction taint = isInTaintedSet(arg, in);
                    if (taint != null) {
                        Unit source = taint.getSource();
                        reporter.report(method, source, d);
//                        if(source.hasTag(Tainted.NAME)) {
//                            Tag tainted = source.getTag(Tainted.NAME);
//                            System.out.println(Tainted.NAME + " " + new String(tainted.getValue()));
//                        }
                    }
                }
            }
        }
    }

    /**
     * Checks, if a tainted Local is assigned to another Local or a class field
     * @param in
     * @param d
     * @param out
     */
    private void checkAssignmentOfTaintedLocal(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if (d instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt def = (AbstractDefinitionStmt) d;
            Value leftSide = def.getLeftOp();
            Value rightSide = def.getRightOp();
            if ((leftSide instanceof Local || leftSide instanceof FieldRef) && rightSide instanceof Local) {
                for (Iterator<FlowAbstraction> iterator = in.iterator(); iterator.hasNext();) {
                    FlowAbstraction taint  = iterator.next();
                    if(taint.getLocal().equals(rightSide)) {
                        d.addTag(new Tainted("assignment of tainted local"));
                        taint(leftSide, d, out);
                    }
                }
            }
        }
    }

    /**
     * Checks, if a previously tainted Local is overwritten with a Constant.
     * In this case, the Local is not tainted anymore and is not propagated in the out set
     * @param in
     * @param d
     * @param out
     */
    private void checkTaintOverwrite(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        // we are interested in assignments only 
        if (d instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt def = (AbstractDefinitionStmt) d;
            Value leftSide = def.getLeftOp();
            Value rightSide = def.getRightOp();
            // only check assignments, where the left side is a Local and the right side is a Constant
            if (leftSide instanceof Local && rightSide instanceof Constant) {
                d.removeTag(Tainted.NAME);
                Local untainted = (Local) leftSide;
                // now check, if the Local was tainted previously and remove it from the out set
                FlowAbstraction taint = isInTaintedSet(untainted, in);
                if(taint != null) {
                    in.remove(taint);
                }
            }
        }
    }

    /**
     * Checks, if the return value of a method with the name containing "getSecret" is assigned to a Local.
     * If that is the case, the Local is marked as tainted.
     * @param in
     * @param d
     * @param out
     */
    private void checkGetSecret(Set<FlowAbstraction> in, Unit d, Set<FlowAbstraction> out) {
        if (d instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt def = (AbstractDefinitionStmt) d;
            Value rightSide = def.getRightOp();
            Value leftSide = def.getLeftOp();
            if (leftSide instanceof Local && rightSide instanceof InvokeExpr) {
                InvokeExpr call = (InvokeExpr) rightSide;
                String methodName = call.getMethod().getName();
                if (methodName.contains("getSecret")) {
                    // left side is tainted
                    // add tag with cause for the taint
                    //d.addTag(new Tainted("call to getSecret()"));
                    out.add(new FlowAbstraction(d, (Local)leftSide));
                }
            }
        }
    }
    
    /**
     * Marks a Local or FieldRef as tainted. If a FieldRef is marked as tainted, the
     * declaring class is tagged with the Tainted tag, to mark it as tainted, too.
     * @param v
     * @param d
     * @param out
     */
    private void taint(Value v, Unit d, Set<FlowAbstraction> out) {
        if(v instanceof Local) {
            out.add(new FlowAbstraction(d, (Local)v));
        } else {
            FieldRef ref = (FieldRef) v;
            // add a tag to the declaring class, which marks it tainted
            ref.getField().getDeclaringClass().addTag(new Tainted("class contains tainted field [" + ref.getField().getName()+"]"));
            // add the field to the set of taints
            out.add(new FlowAbstraction(d, ref.getField()));
        }
    }
    
    /**
     * Checks, if a given element (Local, FieldRef, etc.) is tainted. If it is tainted, the
     * FlowAbstraction containing the element and the source Unit is returned. 
     * Otherwise <code>null</code> is returned
     * @param o
     * @param taintedSet
     * @return
     */
    private FlowAbstraction isInTaintedSet(Object o, Set<FlowAbstraction> taintedSet) {
        for (Iterator<FlowAbstraction> iterator = taintedSet.iterator(); iterator.hasNext();) {
            FlowAbstraction taint  = iterator.next();
            
            if(o instanceof Local) {
                if(taint.getLocal() != null && taint.getLocal().equals(o)) {
                    return taint;
                }
            } else if(o instanceof FieldRef) {
                SootField f = ((FieldRef)o).getField();
                if(taint.getField() != null && taint.getField().equals(f)) {
                    return taint;
                }
            }
        }
        return null;
    }

    @Override
    protected Set<FlowAbstraction> newInitialFlow() {
        return new HashSet<FlowAbstraction>();
    }

    @Override
    protected Set<FlowAbstraction> entryInitialFlow() {
        return new HashSet<FlowAbstraction>();
    }

    @Override
    protected void merge(Set<FlowAbstraction> in1, Set<FlowAbstraction> in2, Set<FlowAbstraction> out) {
        out.addAll(in1);
        out.addAll(in2);
    }

    @Override
    protected void copy(Set<FlowAbstraction> source, Set<FlowAbstraction> dest) {
        dest.clear();
        dest.addAll(source);
    }
    
    public void doAnalyis() {
        super.doAnalysis();
    }
    
    /**
     * Custom tag to mark elements as tainted. This
     * is for example used to mark the declaring class of
     * a tainted field as tained.
     */
    public class Tainted implements Tag {
        public static final String NAME = "Tainted";
        private String cause;

        public Tainted(String cause) {
            this.cause = cause;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public byte[] getValue() throws AttributeValueException {
            return cause.getBytes();
        }
    }
}
