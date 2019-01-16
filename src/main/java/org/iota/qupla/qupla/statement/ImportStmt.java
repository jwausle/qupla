package org.iota.qupla.qupla.statement;

import org.iota.qupla.helper.ModuleLoader;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

import java.util.Objects;

public class ImportStmt extends BaseExpr
{
  public QuplaModule importModule;
  private final ModuleLoader moduleLoader;


  private ImportStmt(final ImportStmt copy)
  {
    super(copy);
    importModule = copy.importModule;
    moduleLoader = copy.moduleLoader;
  }

  public ImportStmt(final Tokenizer tokenizer, ModuleLoader moduleLoader)
  {
    super(tokenizer);
    this.moduleLoader = Objects.requireNonNull(moduleLoader,"'moduleLoader' must not be null.");

    expect(tokenizer, Token.TOK_IMPORT, "import");

    final Token firstPart = expect(tokenizer, Token.TOK_NAME, "module name");
    name = firstPart.text;
  }

  @Override
  public void analyze()
  {
    importModule = QuplaModule.parse(name, moduleLoader);
    size = 1;
  }

  @Override
  public BaseExpr clone()
  {
    return new ImportStmt(this);
  }
}
