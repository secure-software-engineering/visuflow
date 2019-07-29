
package de.visuflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import heros.DefaultSeeds;
import heros.EdgeFunction;
import heros.EdgeFunctions;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import soot.Local;
import soot.NullType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import static heros.TwoElementSet.twoElementSet;

public class InterProceduralAnalysis extends  DefaultJimpleIDETabulationProblem<Local, Integer, InterproceduralCFG<Unit, SootMethod>>
	{

    public InterProceduralAnalysis(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
    }

    public static final Integer TOP_ELEMENT = new Integer(1);
    public static final Integer BOTTOM_ELEMENT = new Integer(0);

    @Override
    protected EdgeFunction<Integer> createAllTopFunction() {
        // TODO: Implement this function to return a special EdgeFunction that
        // represents 'no information' at all, that is used to initialize the
        // data-flow fact's values.
        return new EdgeFunction<Integer>() {
            @Override
            public Integer computeTarget(Integer source) {
                return BOTTOM_ELEMENT;
            }

            @Override
            public EdgeFunction<Integer> composeWith(EdgeFunction<Integer> secondFunction) {
                return secondFunction;
            }

            @Override
            public EdgeFunction<Integer> joinWith(EdgeFunction<Integer> otherFunction) {
                return otherFunction;
            }

            @Override
            public boolean equalTo(EdgeFunction<Integer> other) {
                return this.getClass() == other.getClass();
            }
        };
    }

    @Override
    protected JoinLattice<Integer> createJoinLattice() {
        return new JoinLattice<Integer>() {
            @Override
            public Integer topElement() {
                return BOTTOM_ELEMENT;
            }

            @Override
            public Integer bottomElement() {
                return TOP_ELEMENT;
            }

            @SuppressWarnings("NumberEquality")
            @Override
            public Integer join(Integer left, Integer right) {
                if (left == BOTTOM_ELEMENT && right == BOTTOM_ELEMENT) {
                    return BOTTOM_ELEMENT;
                } else if (left == TOP_ELEMENT || right == TOP_ELEMENT) {
                    return TOP_ELEMENT;
                } else if (left == BOTTOM_ELEMENT) {
                    return right;
                } else if (right == BOTTOM_ELEMENT) {
                    return left;
                } else if (left.equals(right)) {
                    return left;
                }

                return TOP_ELEMENT;
            }
        };
    }

    @Override
    public Map<Unit, Set<Local>> initialSeeds() {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (!m.hasActiveBody()) {
                    continue;
                }
                if (m.getName().equals("entryPoint")) {
                    return DefaultSeeds.make(Collections.singleton(m.getActiveBody().getUnits().getFirst()), zeroValue());
                }
            }
        }
        throw new IllegalStateException("scene does not contain 'entryPoint'");
    }

    @Override
    protected FlowFunctions<Unit, Local, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, Local, SootMethod>() {
            @Override
            public FlowFunction<Local> getNormalFlowFunction(Unit curr, Unit succ) {
                // TODO: Implement this flow function factory to obtain an intra-procedural data-flow analysis.
  
                if (curr instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) curr;

                    if (stmt.getLeftOp() instanceof Local) {
                        Local targetLocal = (Local) stmt.getLeftOp();

                        if (stmt.getRightOp() instanceof IntConstant) {
                            // Example: foo = 17;
                            return source -> {
                                if (source.equals(targetLocal)) {
                                    return Collections.emptySet();
                                } else if (source.equals(zeroValue())) {
                                    return Collections.singleton(targetLocal);
                                } else {
                                    return Collections.singleton(source);
                                }
                            };
                        } else if (stmt.getRightOp() instanceof Local) {
                            // Example: foo = bar;
                            Local rightLocal = (Local) stmt.getRightOp();

                            return source -> {
                                if (source.equals(rightLocal)) {
                                    return twoElementSet(source, targetLocal);
                                } else if (source.equals(targetLocal)) {
                                    return Collections.emptySet();
                                } else {
                                    return Collections.singleton(source);
                                }
                            };
                        } else if (stmt.getRightOp() instanceof BinopExpr) {
                            // Examples: 'foo = 17 + 3', 'foo = 17 + bar', 'foo = foo + bar';
                            BinopExpr operation = (BinopExpr) stmt.getRightOp();

                            if (operation.getOp1() instanceof IntConstant) {
                                if (operation.getOp2() instanceof IntConstant) {
                                    // 'foo = 17 + 3'
                                    return source -> {
                                        if (source.equals(zeroValue())) {
                                            return twoElementSet(source, targetLocal);
                                        } else if (source.equals(targetLocal)) {
                                            return Collections.emptySet();
                                        } else {
                                            return Collections.singleton(source);
                                        }
                                    };

                                } else if (operation.getOp2() instanceof Local) {
                                    // 'foo = 17 + bar'
                                    Local b = (Local) operation.getOp2();
                                    return source -> {
                                        Set<Local> resultFacts = new HashSet<>();
                                        if (!source.equals(targetLocal)) {
                                            resultFacts.add(source);
                                        }
                                        if (source.equals(b)) {
                                            resultFacts.add(targetLocal);
                                        }
                                        return resultFacts;
                                    };
                                }
                            } else if (operation.getOp1() instanceof Local && operation.getOp2() instanceof IntConstant) {
                                // 'foo = bar + 17'
                                Local a = (Local) operation.getOp1();
                                return source -> {
                                    Set<Local> resultFacts = new HashSet<>();
                                    if (!source.equals(targetLocal)) {
                                        resultFacts.add(source);
                                    }
                                    if (source.equals(a)) {
                                        resultFacts.add(targetLocal);
                                    }
                                    return resultFacts;
                                };
                            }
                        }

                        // In all other assignment cases (like 'foo = foo + bar') kill the fact
                        return source -> source.equals(targetLocal)
                                ? Collections.emptySet()
                                : Collections.singleton(source);
                    }
                }

                return Identity.v();
            }

            @Override
            public FlowFunction<Local> getCallFlowFunction(Unit callStmt, SootMethod dest) {
                // TODO: Implement this flow function factory to map the actual into the formal arguments.
                // Caution, actual parameters may be integer literals as well.
            	
                if (!dest.hasActiveBody())
                    return Identity.v();

                InvokeExpr invokeExpr;

                if (callStmt instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) callStmt;
                    if (stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof InvokeExpr) {
                        invokeExpr = ((AssignStmt) callStmt).getInvokeExpr();
                    } else {
                        invokeExpr = null;
                    }
                } else if (callStmt instanceof InvokeStmt) {
                    invokeExpr = ((InvokeStmt) callStmt).getInvokeExpr();
                } else {
                    invokeExpr = null;
                }

                if (invokeExpr != null) {
                    return source -> {
                        Set<Local> resultFacts = new HashSet<>();
                        resultFacts.add(zeroValue());

                        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
                            Local parameterLocal = dest.getActiveBody().getParameterLocal(i);
                            Value argument = invokeExpr.getArg(i);
                            if (argument instanceof IntConstant && source.equals(zeroValue()) || argument.equals(source)) {
                                resultFacts.add(parameterLocal);
                            }
                        }

                        return resultFacts;
                    };
                }

                return Identity.v();
            }

            @Override
            public FlowFunction<Local> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit
                    returnSite) {
            	
            	// TODO: Map the return value back into the caller's context if applicable.
                // Since Java has pass-by-value semantics for primitive data types, you do not have to map the formals
                // back to the actuals at the exit of the callee.
                return source -> {
                    if (callSite instanceof AssignStmt) {
                        AssignStmt stmt = (AssignStmt) callSite;
                        if (stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof InvokeExpr) {
                            Local targetLocal = (Local) stmt.getLeftOp();
                            Local resultLocal = calleeMethod.getActiveBody().getLocals().getLast();

                            if (source.equals(resultLocal))
                                return Collections.singleton(targetLocal);
                        }
                    }

                    // kill all local data flow facts when returning to caller
                    return Collections.emptySet();
                };
            }

            @Override
            public FlowFunction<Local> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                // TODO: getCallToReturnFlowFunction can be left to return id in many analysis; this time as well?
                // since Java uses call-by-value and we only consider locals the called method cannot change the facts for the caller
            	
                return Identity.v();
            }
        }

                ;
    }

    @Override
    protected EdgeFunctions<Unit, Local, SootMethod, Integer> createEdgeFunctionsFactory() {
        return new EdgeFunctions<Unit, Local, SootMethod, Integer>() {
            @Override
            public EdgeFunction<Integer> getNormalEdgeFunction(Unit src, Local srcNode, Unit tgt, Local tgtNode) {
                if (tgtNode.equals(zeroValue()))
                    return EdgeIdentity.v();

                if (src instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) src;
                    Local targetLocal = (Local) assignStmt.getLeftOp();

                    if (!tgtNode.equals(targetLocal)) {
                        return EdgeIdentity.v();
                    } else if (assignStmt.getRightOp() instanceof IntConstant) {
                        IntConstant constant = (IntConstant) assignStmt.getRightOp();
                        return new LinearEquationEdgeFunction(0, constant.value);
                    } else if (assignStmt.getRightOp() instanceof Local) {
                        Local rightOp = (Local) assignStmt.getRightOp();
                        return new LinearEquationEdgeFunction(1, 0);
                    } else if (assignStmt.getRightOp() instanceof BinopExpr) {
                        BinopExpr expr = (BinopExpr) assignStmt.getRightOp();
                        if (srcNode.equals(zeroValue())) {
                            IntConstant a = (IntConstant) expr.getOp1();
                            IntConstant b = (IntConstant) expr.getOp2();
                            int result = evaluate(expr.getSymbol(), a.value, b.value);
                            return new LinearEquationEdgeFunction(0, result);
                        }

                        switch (expr.getSymbol().trim()) {
                            case "+": {
                                IntConstant constant = (IntConstant) ((expr.getOp1() instanceof IntConstant)
                                        ? expr.getOp1()
                                        : expr.getOp2());
                                return new LinearEquationEdgeFunction(1, constant.value);
                            }

                            case "-": {
                                if (expr.getOp1() instanceof IntConstant) {
                                    // e.g. 'j = 5 - i'
                                    int constant = ((IntConstant) expr.getOp1()).value;
                                    return new LinearEquationEdgeFunction(-1, constant);
                                } else {
                                    // e.g. 'j = i - 5'
                                    int constant = ((IntConstant) expr.getOp2()).value;
                                    return new LinearEquationEdgeFunction(1, -constant);
                                }
                            }

                            case "*": {
                                IntConstant constant = (IntConstant) ((expr.getOp1() instanceof IntConstant)
                                        ? expr.getOp1()
                                        : expr.getOp2());
                                return new LinearEquationEdgeFunction(constant.value, 0);
                            }

                            case "/": {
                                if (expr.getOp1() instanceof IntConstant) {
                                    // e.g. 'j = 5 / i' (not a linear expression)
                                    return new TopEdgeFunction();
                                } else {
                                    // e.g. 'j = i / 5' <=> 'j = .25 * i'
                                    BigDecimal constant = new BigDecimal(((IntConstant) expr.getOp2()).value);

                                    if (constant.equals(BigDecimal.ZERO)) return new TopEdgeFunction();
                                    BigDecimal factor = BigDecimal.ONE.divide(constant, RoundingMode.HALF_EVEN);
                                    return new LinearEquationEdgeFunction(factor, 0);
                                }
                            }
                        }
                    }
                }
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getCallEdgeFunction(Unit callStmt, Local srcNode, SootMethod destinationMethod, Local destNode) {
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Local exitNode, Unit returnSite, Local retNode) {
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getCallToReturnEdgeFunction(Unit callStmt, Local callNode, Unit returnSite, Local returnSideNode) {
                return EdgeIdentity.v();
            }
        };
    }

    private static int evaluate(String symbol, int a, int b) {
        switch (symbol.trim()) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
            case "/":
                return a / b;
            default:
                throw new UnsupportedOperationException(symbol.trim());
        }
    }

    @Override
    protected JimpleLocal createZeroValue() {
        return new JimpleLocal("<<zero>>", NullType.v());
    }
}
