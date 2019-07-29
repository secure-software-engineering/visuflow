
package de.visuflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.istack.internal.Nullable;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;
import heros.solver.Pair;
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
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import static heros.TwoElementSet.twoElementSet;


public class InterProceduralAnalysis
    extends DefaultJimpleIFDSTabulationProblem<Pair<Local, Integer>, InterproceduralCFG<Unit, SootMethod>>
{

    protected final static int LOWER_BOUND = -1000;
    protected final static int UPPER_BOUND = 1000;
    
    public InterProceduralAnalysis(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
    }

    @Override
    public Map<Unit, Set<Pair<Local, Integer>>> initialSeeds() {
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
    protected FlowFunctions<Unit, Pair<Local, Integer>, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, Pair<Local, Integer>, SootMethod>() {
            @Override
            public FlowFunction<Pair<Local, Integer>> getNormalFlowFunction(Unit curr, Unit next) {
                // TODO: Implement this flow function factory to obtain an intra-procedural data-flow analysis.
            	if (curr instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) curr;
                   
                    if (stmt.getLeftOp() instanceof Local) {
                        Local targetLocal = (Local) stmt.getLeftOp();

                        if (stmt.getRightOp() instanceof IntConstant) {
                            // Example: foo = 17;
                            IntConstant constant = (IntConstant) stmt.getRightOp();
                            return source -> {
                                if (source.getO1().equals(targetLocal)) {
                                    return Collections.emptySet();
                                } else if (source.equals(zeroValue())) {
                                    return Collections.singleton(new Pair<>(targetLocal, constant.value));
                                } else {
                                    return Collections.singleton(source);
                                }
                            };
                        } else if (stmt.getRightOp() instanceof Local) {
                            // Example: foo = bar;
                            Local rightLocal = (Local) stmt.getRightOp();

                            return source -> {
                                if (source.getO1().equals(rightLocal)) {
                                    return twoElementSet(source, new Pair<>(targetLocal, source.getO2()));
                                } else if (source.getO1().equals(targetLocal)) {
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
                                    int a = ((IntConstant) operation.getOp1()).value;
                                    int b = ((IntConstant) operation.getOp2()).value;
                                    Integer result = evaluate(operation.getSymbol(), a, b);
                                    if (result != null) {
                                        return source -> {
                                            if (source.equals(zeroValue())) {
                                                return twoElementSet(source, new Pair<>(targetLocal, result));
                                            } else if (source.getO1().equals(targetLocal)) {
                                                return Collections.emptySet();
                                            } else {
                                                return Collections.singleton(source);
                                            }
                                        };
                                    }
                                } else if (operation.getOp2() instanceof Local) {
                                    // 'foo = 17 + bar'
                                    int a = ((IntConstant) operation.getOp1()).value;
                                    Local b = (Local) operation.getOp2();
                                    return source -> {
                                        Set<Pair<Local, Integer>> resultFacts = new HashSet<>();
                                        if (!source.getO1().equals(targetLocal)) {
                                            resultFacts.add(source);
                                        }
                                        if (source.getO1().equals(b)) {
                                            Integer result = evaluate(operation.getSymbol(), a, source.getO2());
                                            if (result != null)
                                                resultFacts.add(new Pair<>(targetLocal, result));
                                        }
                                        return resultFacts;
                                    };
                                }
                            } else if (operation.getOp1() instanceof Local && operation.getOp2() instanceof IntConstant) {
                                // 'foo = bar + 17'
                                Local a = (Local) operation.getOp1();
                                int b = ((IntConstant) operation.getOp2()).value;
                                return source -> {
                                    Set<Pair<Local, Integer>> resultFacts = new HashSet<>();
                                    if (!source.getO1().equals(targetLocal)) {
                                        resultFacts.add(source);
                                    }
                                    if (source.getO1().equals(a)) {
                                        Integer result = evaluate(operation.getSymbol(), source.getO2(), b);
                                        if (result != null)
                                            resultFacts.add(new Pair<>(targetLocal, result));
                                    }
                                    return resultFacts;
                                };
                            }
                        }

                        // In all other assignment cases (like 'foo = foo + bar') kill the fact
                        return source -> source.getO1().equals(targetLocal)
                                ? Collections.emptySet()
                                : Collections.singleton(source);
                    }
                }

                return Identity.v();
            }

            @Override
            public FlowFunction<Pair<Local, Integer>> getCallFlowFunction(Unit callsite, SootMethod dest) {
                // TODO: Implement this flow function factory to map the actual into the formal arguments.
                // Caution, actual parameters may be integer literals as well.

                if (!dest.hasActiveBody())
                    return Identity.v();

                InvokeExpr invokeExpr;

                if (callsite instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) callsite;
                    if (stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof InvokeExpr) {
                        invokeExpr = ((AssignStmt) callsite).getInvokeExpr();
                    } else {
                        invokeExpr = null;
                    }
                } else if (callsite instanceof InvokeStmt) {
                    invokeExpr = ((InvokeStmt) callsite).getInvokeExpr();
                } else {
                    invokeExpr = null;
                }

                if (invokeExpr != null) {
                    return source -> {
                        Set<Pair<Local, Integer>> resultFacts = new HashSet<>();
                        resultFacts.add(zeroValue());

                        if (source.equals(zeroValue())) {
                            // create a fact for each integer constant arguments
                            for (int i = 0; i < invokeExpr.getArgCount(); i++) {
                                Local parameterLocal = dest.getActiveBody().getParameterLocal(i);
                                Value argument = invokeExpr.getArg(i);
                                if (argument instanceof IntConstant) {
                                    int argConstant = ((IntConstant) argument).value;
                                    resultFacts.add(new Pair<>(parameterLocal, argConstant));
                                }
                            }
                        } else {
                            for (int i = 0; i < invokeExpr.getArgCount(); i++) {
                                Local parameterLocal = dest.getActiveBody().getParameterLocal(i);
                                Value argument = invokeExpr.getArg(i);

                                if (argument.equals(source.getO1())) {
                                    Local argLocal = (Local) argument;
                                    resultFacts.add(new Pair<>(parameterLocal, source.getO2()));
                                }
                            }
                        }

                        return resultFacts;
                    };
                }

                return Identity.v();
            }

            @Override
            public FlowFunction<Pair<Local, Integer>> getReturnFlowFunction(Unit callsite, SootMethod callee, Unit
                    exit, Unit retsite) {
                // TODO: Map the return value back into the caller's context if applicable.
                // Since Java has pass-by-value semantics for primitive data types, you do not have to map the formals
                // back to the actuals at the exit of the callee.

                return source -> {
                    if (callsite instanceof AssignStmt) {
                        AssignStmt stmt = (AssignStmt) callsite;
                        if (stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof InvokeExpr) {
                            Local targetLocal = (Local) stmt.getLeftOp();
                            Local resultLocal = callee.getActiveBody().getLocals().getLast();

                            if (source.getO1().equals(resultLocal))
                                return Collections.singleton(new Pair<>(targetLocal, source.getO2()));
                        }
                    }

                    // kill all local data flow facts when returning to caller
                    return Collections.emptySet();
                };
            }

            @Override
            public FlowFunction<Pair<Local, Integer>> getCallToReturnFlowFunction(Unit callsite, Unit retsite) {
                // TODO: getCallToReturnFlowFunction can be left to return id in many analysis; this time as well?
                // since Java uses call-by-value and we only consider locals the called method cannot change the facts for the caller
                return Identity.v();
            }
        }

                ;
    }

    @Override
    protected Pair<Local, Integer> createZeroValue() {
        return new Pair<>(new JimpleLocal("<<zero>>", NullType.v()), Integer.MIN_VALUE);
    }

    @Nullable
    private static Integer evaluate(String symbol, int a, int b) {
        int result;
        switch (symbol.trim()) {
            case "+":
                result = a + b;
                break;
            case "-":
                result = a - b;
                break;
            case "*":
                result = a * b;
                break;
            case "/":
                result = a / b;
                break;
            default:
                return null;
        }
        return (result < LOWER_BOUND || result > UPPER_BOUND) ? null : result;
    }
}
