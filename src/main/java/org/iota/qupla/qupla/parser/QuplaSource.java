package org.iota.qupla.qupla.parser;

import org.iota.qupla.helper.ModuleLoader;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.statement.ExecStmt;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.ImportStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.TemplateStmt;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;

import java.util.Objects;
public class QuplaSource extends BaseExpr
{
  private final ModuleLoader moduleLoader;
  public String pathName;

  public QuplaSource(final Tokenizer tokenizer, final String pathName, ModuleLoader moduleLoader)
  {
    super(tokenizer);
    this.moduleLoader = Objects.requireNonNull(moduleLoader,"'moduleLoader' must not be null.");
    this.pathName = pathName;
    module.currentSource = this;

    origin = tokenizer.nextToken();

    while (tokenizer.tokenId() != Token.TOK_EOF)
    {
      switch (tokenizer.tokenId())
      {
      case Token.TOK_EVAL:
        module.execs.add(new ExecStmt(tokenizer, false));
        break;

      case Token.TOK_FUNC:
        module.funcs.add(new FuncStmt(tokenizer));
        break;

      case Token.TOK_IMPORT:
        module.imports.add(new ImportStmt(tokenizer, moduleLoader));
        break;

      case Token.TOK_LUT:
        module.luts.add(new LutStmt(tokenizer));
        break;

      case Token.TOK_TEMPLATE:
        module.templates.add(new TemplateStmt(tokenizer));
        break;

      case Token.TOK_TEST:
        module.execs.add(new ExecStmt(tokenizer, true));
        break;

      case Token.TOK_TYPE:
        module.types.add(new TypeStmt(tokenizer));
        break;

      case Token.TOK_USE:
        module.uses.add(new UseStmt(tokenizer));
        break;

      default:
        final Token token = tokenizer.currentToken();
        error(token, "Unexpected token: '" + token.text + "'");
      }
    }
  }

  @Override
  public void analyze()
  {
  }

  @Override
  public BaseExpr clone()
  {
    return null;
  }

  @Override
  public String toString()
  {
    return pathName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QuplaSource source = (QuplaSource) o;
    return Objects.equals(moduleLoader, source.moduleLoader) &&
            Objects.equals(pathName, source.pathName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleLoader, pathName);
  }
}
