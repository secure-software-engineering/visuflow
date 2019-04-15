package de.userstudy.analysis1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class Analysis extends ForwardFlowAnalysis<Unit, Set<DataFlowFact>> {

  public int flowThroughCount = 0;
  private final SootMethod method;
  private final Reporter reporter;

  private enum OutFlowType {
    Parameter, ThisLocal, ReturnValue
  };

  private Set<DataFlowFact> initialFlow = null;
  private Map<DataFlowFact, Set<OutFlowType>> outFlows = null;
  private final Map<SootMethod, Set<DataFlowFact>> visitedMethods;

  /**
   * Creates a new instance of the {@link Analysis} class
   * 
   * @param b
   *          The body on which to conduct the taint analysis
   * @param reporter
   *          The reporting object to be used for reporting the data leaks that
   *          have been found
   */
  public Analysis(Body b, Reporter reporter) {
    this(b, reporter, new HashMap<SootMethod, Set<DataFlowFact>>());
  }

  /**
   * Creates a new instance of the {@link Analysis} class
   * 
   * @param b
   *          The body on which to conduct the taint analysis
   * @param reporter
   *          The reporting object to be used for reporting the data leaks that
   *          have been found
   * @param visitedMethods
   *          The methods that have already been visited together with the
   *          incoming abstractions that have already been seen for these
   *          methods.
   */
  public Analysis(Body b, Reporter reporter,
      Map<SootMethod, Set<DataFlowFact>> visitedMethods) {
    super(new ExceptionalUnitGraph(b));
    this.method = b.getMethod();
    this.visitedMethods = visitedMethods;
    this.reporter = reporter;
  }

  protected void flowThrough(Set<DataFlowFact> in, Unit d,
      Set<DataFlowFact> out) {

    // Check for source and sink calls
    if (((Stmt) d).containsInvokeExpr()) {
      InvokeExpr inv = ((Stmt) d).getInvokeExpr();

      // Check for a call to the source
      if (d instanceof DefinitionStmt
          && inv.getMethod().getName().equals("getSecret")) {
        Value leftOp = ((DefinitionStmt) d).getLeftOp();
        out.add(new DataFlowFact(d, (Local) leftOp));
      }
      // Check whether a tainted value is passed to a dangerousTransmission
      else if (inv.getMethod().getName().equals("dangerousTransmission")) {
        for (DataFlowFact abs : in)
          if (inv.getArgs().contains(abs.getLocal())) {
            reporter.report(method, abs.getSource(), d);
          }
      }
      // Check for other method invocation
      else spawnAnalysisForCallee((Stmt) d, in, out);
    }

    out.addAll(in);

    // If this is a return statement, we save the outbound flow
    if (d instanceof ReturnStmt) {
      ReturnStmt retStmt = (ReturnStmt) d;
      for (DataFlowFact ap : in) {
        if (retStmt.getOp() == ap.getLocal())
          addOutFlow(ap, OutFlowType.ReturnValue);
      }
    }

    // If we leave a method, we need to save the parameter and "this" taints
    if (d instanceof ReturnStmt || d instanceof ReturnVoidStmt) {
      for (DataFlowFact ust : in) {
        for (Local paramLocal : method.getActiveBody().getParameterLocals())
          if (paramLocal == ust.getLocal()) {
            addOutFlow(ust, OutFlowType.Parameter);
            break;
          }
        if (!this.method.isStatic()
            && ust.getLocal() == method.getActiveBody().getThisLocal())
          addOutFlow(ust, OutFlowType.ThisLocal);
      }
    }

    for (DataFlowFact ust : in) {
      if (d instanceof AssignStmt) {
        AssignStmt assign = (AssignStmt) d;
        if (assign.getLeftOp() instanceof Local) {
          if (assign.getRightOp() instanceof InstanceFieldRef) {
            // a = b.f and ust = b.f.*
            InstanceFieldRef fr = (InstanceFieldRef) assign.getRightOp();
            if (ust.getLocal() == fr.getBase()
                && ust.getFirstField() == fr.getField()) {
              out.add(ust.deriveWithNewLocalAndPopFirstField(
                  (Local) assign.getLeftOp()));
            }
          } else if (assign.getRightOp() instanceof Local) {
            // a = b and ust = b.*
            if (ust.getLocal() == assign.getRightOp()) {
              out.add(ust.deriveWithNewLocal((Local) assign.getLeftOp()));
            }
          }

        } else if (assign.getLeftOp() instanceof InstanceFieldRef) {
          InstanceFieldRef fr = (InstanceFieldRef) assign.getLeftOp();
          Value base = fr.getBase();
          SootField field = fr.getField();
          // a.f = c; ust = c.*
          if (ust.getLocal() == assign.getRightOp() && base instanceof Local) {
            out.add(ust.deriveWithNewLocalAndAppendField((Local) base, field));
          }
          if (assign.getRightOp() instanceof Constant) {
            // a = b and ust = b.*
            out.remove(
                ust.deriveWithNewLocalAndAppendField((Local) base, field));
          }
        }
      }
    }
  }

  private void addOutFlow(DataFlowFact ap, OutFlowType type) {
    if (this.outFlows == null) this.outFlows = new HashMap<>();

    Set<OutFlowType> types = this.outFlows.get(ap);
    if (types == null) {
      types = new HashSet<>();
      this.outFlows.put(ap, types);
    }

    types.add(type);
  }

  private void spawnAnalysisForCallee(Stmt stmt, Set<DataFlowFact> in,
      Set<DataFlowFact> out) {
    assert stmt.containsInvokeExpr();
    InvokeExpr invExpr = stmt.getInvokeExpr();

    for (Iterator<Edge> calleeIt = Scene.v().getCallGraph()
        .edgesOutOf(stmt); calleeIt.hasNext();) {
      SootMethod callee = calleeIt.next().tgt();
      if (!callee.hasActiveBody()) continue;

      // Do not run in circles
      Map<SootMethod, Set<DataFlowFact>> newVisited = new HashMap<>(
          visitedMethods);
      Set<DataFlowFact> visitedAPs = newVisited.get(callee);
      if (visitedAPs == null) {
        visitedAPs = new HashSet<>();
        newVisited.put(callee, visitedAPs);
      } else if (subset(visitedAPs, in)) continue;
      visitedAPs.addAll(in);

      Analysis ipa = new Analysis(callee.getActiveBody(), reporter, newVisited);

      // Map the parameters over
      if (!callee.getName().equals("<clinit>"))
        for (int paramIdx = 0; paramIdx < invExpr.getArgCount(); paramIdx++) {
        for (DataFlowFact ap : in) {
        if (ap.getLocal() == invExpr.getArg(paramIdx)) {
        Local calleeLocal = callee.getActiveBody().getParameterLocal(paramIdx);
        ipa.addInitialFlow(ap.deriveWithNewLocal(calleeLocal));
        }
        }
        }

      // Map the base object over
      if (invExpr instanceof InstanceInvokeExpr) {
        InstanceInvokeExpr iinvExpr = (InstanceInvokeExpr) invExpr;
        for (DataFlowFact ap : in) {
          if (ap.getLocal() == iinvExpr.getBase()) {
            Local calleeThis = callee.getActiveBody().getThisLocal();
            ipa.addInitialFlow(ap.deriveWithNewLocal(calleeThis));
          }
        }
      }

      // Spawn the new analysis
      ipa.doAnalyis();

      // Map the results back
      if (ipa.getOutFlows() != null)
        for (Entry<DataFlowFact, Set<OutFlowType>> outEntry : ipa.getOutFlows()
            .entrySet()) {
        DataFlowFact outAP = outEntry.getKey();

        // Is this a parameter local?
        if (outEntry.getValue().contains(OutFlowType.Parameter)) for (int paramIdx = 0; paramIdx < callee.getParameterCount(); paramIdx++) {
        Local l = callee.getActiveBody().getParameterLocal(paramIdx);
        if (l == outAP.getLocal()) out.add(outAP.deriveWithNewLocal((Local) invExpr.getArg(paramIdx)));
        }

        // Map back taints on the "this" local
        if (outEntry.getValue().contains(OutFlowType.ThisLocal) && invExpr instanceof InstanceInvokeExpr) {
        InstanceInvokeExpr iinvExpr = (InstanceInvokeExpr) invExpr;
        out.add(outAP.deriveWithNewLocal((Local) iinvExpr.getBase()));
        }

        // Map the return values back
        if (stmt instanceof DefinitionStmt) {
        DefinitionStmt defStmt = (DefinitionStmt) stmt;
        if (outEntry.getValue().contains(OutFlowType.ReturnValue)) out.add(outAP.deriveWithNewLocal((Local) defStmt.getLeftOp()));
        }
        }
    }
  }

  /**
   * Checks whether set2 is a subset of set1
   * 
   * @param set1
   * @param set2
   * @return
   */
  private <N> boolean subset(Set<N> set1, Set<N> set2) {
    for (N n : set2)
      if (!set1.contains(n)) return false;
    return true;
  }

  @Override
  protected Set<DataFlowFact> newInitialFlow() {
    return new HashSet<DataFlowFact>();
  }

  @Override
  protected Set<DataFlowFact> entryInitialFlow() {
    return initialFlow == null ? new HashSet<DataFlowFact>() : initialFlow;
  }

  private void addInitialFlow(DataFlowFact ap) {
    if (this.initialFlow == null) this.initialFlow = new HashSet<>();
    this.initialFlow.add(ap);
  }

  private Map<DataFlowFact, Set<OutFlowType>> getOutFlows() {
    return this.outFlows;
  }

  @Override
  protected void merge(Set<DataFlowFact> in1, Set<DataFlowFact> in2,
      Set<DataFlowFact> out) {
    out.addAll(in1);
    out.addAll(in2);
  }

  @Override
  protected void copy(Set<DataFlowFact> source, Set<DataFlowFact> dest) {
    dest.clear();
    dest.addAll(source);
  }

  public void doAnalyis() {
    super.doAnalysis();
  }

}