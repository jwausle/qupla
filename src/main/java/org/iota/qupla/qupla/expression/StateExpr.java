package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class StateExpr extends BaseExpr
{
  public BaseExpr stateType;
  public TritVector zero;

  private StateExpr(final StateExpr copy)
  {
    super(copy);

    stateType = clone(copy.stateType);
    zero = copy.zero == null ? null : new TritVector(copy.zero);
  }

  public StateExpr(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_STATE, "state");

    stateType = new ConstTypeName(tokenizer);

    final Token varName = expect(tokenizer, Token.TOK_NAME, "variable name");
    name = varName.text;

    for (int i = scope.size() - 1; i >= 0; i--)
    {
      final BaseExpr var = scope.get(i);
      if (var.name.equals(name))
      {
        error("Duplicate variable name: " + name);
      }
    }

    stackIndex = scope.size();
    scope.add(this);
  }

  @Override
  public void analyze()
  {
    stateType.analyze();
    size = stateType.size;
    typeInfo = stateType.typeInfo;

    zero = new TritVector(size, '0');

    scope.add(this);
  }

  @Override
  public BaseExpr clone()
  {
    return new StateExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalState(this);
  }
}
