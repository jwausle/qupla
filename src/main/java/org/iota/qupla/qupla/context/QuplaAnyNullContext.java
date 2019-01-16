package org.iota.qupla.qupla.context;

import java.util.HashSet;
import java.util.Stack;

import org.iota.qupla.Qupla;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.ConcatExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.LutExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.SliceExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.TypeExpr;
import org.iota.qupla.qupla.expression.VectorExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;

public class QuplaAnyNullContext extends QuplaBaseContext
{
  private static final boolean allowLog = false;
  private static final HashSet<FuncStmt> inspected = new HashSet<>();
  private static final HashSet<FuncStmt> inspecting = new HashSet<>();

  private boolean isNull;
  private final Stack<Boolean> stack = new Stack<>();
  private int stackFrame;

  public QuplaAnyNullContext()
  {
  }

  @Override
  public void eval(final QuplaModule module)
  {
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    assign.expr.eval(this);
    stack.push(isNull);
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    // if any expr is non-null the entire concat is non-null
    concat.lhs.eval(this);
    if (!isNull || concat.rhs == null)
    {
      return;
    }

    concat.rhs.eval(this);
  }

  @Override
  public void evalConditional(final CondExpr conditional)
  {
    conditional.condition.eval(this);
    if (isNull || conditional.trueBranch == null)
    {
      // null for nullify() condition parameter on both sides
      // or not a conditional expression
      return;
    }

    // if either expression is non-null the result is non-null
    conditional.trueBranch.eval(this);
    if (isNull && conditional.falseBranch != null)
    {
      conditional.falseBranch.eval(this);
    }
  }

  private void evalFunc(final FuncStmt func)
  {
    if (wasInspected(func))
    {
      // already done
      return;
    }

    if (func.params.size() == 1)
    {
      // easy decision, this falls under all params null rule
      func.anyNull = true;
      return;
    }

    func.anyNull = false;
    if (inspecting.contains(func))
    {
      // recursion detected, cannot determine
      // keep non-null to be on the safe side
      return;
    }

    inspecting.add(func);

    // set all params on stack to false
    for (final BaseExpr param : func.params)
    {
      stack.push(false);
    }

    boolean anyNull = true;
    for (int i = 0; i < func.params.size(); i++)
    {
      stack.set(i, true);

      func.eval(this);
      if (!isNull)
      {
        anyNull = false;
        break;
      }

      stack.set(i, false);
    }

    inspecting.remove(func);

    func.anyNull = anyNull;
    if (!anyNull)
    {
      log("No anyNull for " + func.name);

      // only need to add the ones that are not anyNull
      // the ones that do have definitely been inspected
      inspected.add(func);
    }

    stack.clear();
    stackFrame = 0;
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    for (final BaseExpr stateExpr : func.stateExprs)
    {
      stateExpr.eval(this);
    }

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      assignExpr.eval(this);
    }

    func.returnExpr.eval(this);
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    if (call.args.size() == 1)
    {
      // single argument means the all-null rule is invoked
      // so we return whether the argument was null
      final BaseExpr arg = call.args.get(0);
      arg.eval(this);
      return;
    }

    final boolean currentlyInspecting = inspecting.contains(call.func);
    if (!currentlyInspecting && !wasInspected(call.func))
    {
      // do inspection first
      new QuplaAnyNullContext().evalFunc(call.func);
    }

    int newStackFrame = stack.size();

    if (!pushArguments(call))
    {
      isNull = false;

      // we wait until we know it's not an all null before checking for recursion
      // that way we might be able to decide even with recursion
      if (!currentlyInspecting)
      {
        // no recursion detected
        int oldStackFrame = stackFrame;
        stackFrame = newStackFrame;
        inspecting.add(call.func);
        call.func.eval(this);
        inspecting.remove(call.func);
        stackFrame = oldStackFrame;
      }
    }

    stack.setSize(newStackFrame);
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {

  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    // if any arg is null the result is null
    for (final BaseExpr arg : lookup.args)
    {
      arg.eval(this);
      if (isNull)
      {
        return;
      }
    }
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    // if either expression is non-null the result is non-null
    merge.lhs.eval(this);
    if (merge.rhs == null || !isNull)
    {
      return;
    }

    merge.rhs.eval(this);
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    // a slice of null is null, a slice of non-null is non-null
    isNull = stack.get(stackFrame + slice.stackIndex);
  }

  @Override
  public void evalState(final StateExpr state)
  {
    // of course this is non-null: state vars cannot be null
    isNull = false;
  }

  @Override
  public void evalType(final TypeExpr type)
  {
    // if any field is non-null the entire type is non-null
    for (final BaseExpr expr : type.fields)
    {
      expr.eval(this);
      if (!isNull)
      {
        return;
      }
    }
  }

  @Override
  public void evalVector(final VectorExpr vector)
  {
    // of course this is non-null
    isNull = false;
  }

  private void log(final String text)
  {
    if (allowLog)
    {
      Qupla.log(text);
    }
  }

  private boolean pushArguments(final FuncExpr call)
  {
    boolean allNull = true;
    for (final BaseExpr arg : call.args)
    {
      arg.eval(this);
      if (call.func.anyNull && isNull)
      {
        return true;
      }

      stack.push(isNull);
      allNull &= isNull;
    }

    return allNull;
  }

  private boolean wasInspected(final FuncStmt func)
  {
    return func.anyNull || inspected.contains(func);
  }
}
