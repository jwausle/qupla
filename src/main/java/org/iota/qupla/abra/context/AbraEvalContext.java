package org.iota.qupla.abra.context;

import java.util.Arrays;
import java.util.HashMap;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockImport;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteLatch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.expression.FuncExpr;
import org.iota.qupla.expression.IntegerExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.StateValue;
import org.iota.qupla.helper.TritVector;

public class AbraEvalContext extends AbraCodeContext
{
  private static final HashMap<String, Integer> indexes = new HashMap<>();
  private static final TritVector nullTrit = new TritVector(1);
  // note: stateValues needs to be static so that state is preserved between invocations
  private static final HashMap<StateValue, StateValue> stateValues = new HashMap<>();
  public AbraContext abra;
  public int callNr;
  public byte[] callTrail = new byte[1024];
  public TritVector[] stack;
  public TritVector value;

  public AbraEvalContext()
  {
    if (indexes.size() == 0)
    {
      char key[] = new char[3];
      for (int i = 0; i < 27; i++)
      {
        key[0] = "-01".charAt((i / 1) % 3);
        key[1] = "-01".charAt((i / 3) % 3);
        key[2] = "-01".charAt((i / 9) % 3);
        indexes.put(new String(key), i);
      }
    }
  }

  public void error(final String text)
  {
    throw new CodeException(null, text);
  }

  public void eval(final AbraContext context, final BaseExpr expr)
  {
    abra = context;
    if (expr instanceof FuncExpr)
    {
      final FuncExpr funcExpr = (FuncExpr) expr;
      for (final AbraBlockBranch branch : context.abra.branches)
      {
        if (branch.origin == funcExpr.func)
        {
          value = new TritVector();
          for (final BaseExpr arg : funcExpr.args)
          {
            if (arg instanceof IntegerExpr)
            {
              value = new TritVector(value, ((IntegerExpr) arg).vector);
              continue;
            }

            error("Expected constant value");
          }

          branch.eval(this);
        }
      }
    }
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    final TritVector[] oldStack = stack;
    stack = new TritVector[branch.siteNr];

    for (final AbraSite input : branch.inputs)
    {
      input.append(abra).newline();
      input.eval(this);
    }

    // initialize latches with old values
    for (final AbraSite latch : branch.latches)
    {
      latch.append(abra).newline();
      initializeLatch(latch);
    }

    for (final AbraSite site : branch.sites)
    {
      site.append(abra).newline();
      site.eval(this);
    }

    TritVector result = new TritVector();
    for (final AbraSite output : branch.outputs)
    {
      output.append(abra).newline();
      output.eval(this);
      result = new TritVector(result, value);
    }

    // update latches with new values
    for (final AbraSite latch : branch.latches)
    {
      latch.append(abra).newline();
      updateLatch(latch);
    }

    stack = oldStack;
    value = result;
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    value = new TritVector();
    for (final AbraSite input : knot.inputs)
    {
      // concatenate inputs
      value = new TritVector(value, stack[input.index]);
    }

    if (value.valueTrits == 0)
    {
      stack[knot.index] = new TritVector(knot.size);
      return;
    }

    callTrail[callNr++] = (byte) knot.index;
    knot.block.eval(this);
    callNr--;

    stack[knot.index] = value;
  }

  @Override
  public void evalLatch(final AbraSiteLatch state)
  {
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    if (value.trits.length() != 3)
    {
      error("LUT input size is " + value.trits.length());
    }

    if (value.valueTrits != 3)
    {
      value = nullTrit;
      return;
    }

    final Integer index = indexes.get(value.trits);
    if (index == null)
    {
      value = nullTrit;
      return;
    }

    final char trit = lut.tritCode.buffer[index];
    if (trit == '@')
    {
      value = nullTrit;
      return;
    }

    value = new TritVector("" + trit, 1);
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    value = null;
    for (final AbraSite input : merge.inputs)
    {
      final TritVector mergeValue = stack[input.index];
      if (mergeValue.valueTrits == 0)
      {
        continue;
      }

      if (value == null)
      {
        value = mergeValue;
        continue;
      }

      error("Multiple non-null merge values");
    }

    if (value == null)
    {
      value = new TritVector(merge.size);
    }

    stack[merge.index] = value;
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    if (value.trits.length() < param.offset + param.size)
    {
      error("Insufficient input trits: " + value);
    }

    stack[param.index] = value.slice(param.offset, param.size);
  }

  private void initializeLatch(final AbraSite latch)
  {
    if (latch.references == 0)
    {
      return;
    }

    callTrail[callNr] = (byte) latch.index;

    final StateValue call = new StateValue();
    call.path = callTrail;
    call.pathLength = callNr + 1;

    // if state was saved before set latch to that value otherwise set to zero
    final StateValue stateValue = stateValues.get(call);
    if (stateValue != null)
    {
      stack[latch.index] = stateValue.value;
      return;
    }

    stack[latch.index] = new TritVector(TritVector.zero(latch.size), latch.size);
  }

  private void updateLatch(final AbraSite latch)
  {
    if (latch.references == 0)
    {
      return;
    }

    latch.eval(this);
    stack[latch.index] = value;

    callTrail[callNr] = (byte) latch.index;

    final StateValue call = new StateValue();
    call.path = callTrail;
    call.pathLength = callNr + 1;

    // state already saved?
    final StateValue stateValue = stateValues.get(call);
    if (stateValue != null)
    {
      // reset state?
      if (value.isZero())
      {
        stateValues.remove(call);
        return;
      }

      // overwrite state
      stateValue.value = value;
      return;
    }

    // state not saved yet

    // reset state?
    if (value.isZero())
    {
      // already reset
      return;
    }

    // save state
    call.path = Arrays.copyOf(callTrail, callNr + 1);
    call.value = value;
    stateValues.put(call, call);
  }
}
