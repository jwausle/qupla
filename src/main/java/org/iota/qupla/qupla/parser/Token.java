package org.iota.qupla.qupla.parser;

/**
 * Immutable token representation.
 */
public class Token
{
  public static final int TOK_AFFECT = 1;
  public static final int TOK_ARRAY_CLOSE = TOK_AFFECT + 1;
  public static final int TOK_ARRAY_OPEN = TOK_ARRAY_CLOSE + 1;
  public static final int TOK_COLON = TOK_ARRAY_OPEN + 1;
  public static final int TOK_COMMA = TOK_COLON + 1;
  public static final int TOK_COMMENT = TOK_COMMA + 1;
  public static final int TOK_CONCAT = TOK_COMMENT + 1;
  public static final int TOK_DELAY = TOK_CONCAT + 1;
  public static final int TOK_DIV = TOK_DELAY + 1;
  public static final int TOK_DOT = TOK_DIV + 1;
  public static final int TOK_EOF = TOK_DOT + 1;
  public static final int TOK_EQUAL = TOK_EOF + 1;
  public static final int TOK_EVAL = TOK_EQUAL + 1;
  public static final int TOK_FLOAT = TOK_EVAL + 1;
  public static final int TOK_FUNC = TOK_FLOAT + 1;
  public static final int TOK_FUNC_CLOSE = TOK_FUNC + 1;
  public static final int TOK_FUNC_OPEN = TOK_FUNC_CLOSE + 1;
  public static final int TOK_GROUP_CLOSE = TOK_FUNC_OPEN + 1;
  public static final int TOK_GROUP_OPEN = TOK_GROUP_CLOSE + 1;
  public static final int TOK_IMPORT = TOK_GROUP_OPEN + 1;
  public static final int TOK_JOIN = TOK_IMPORT + 1;
  public static final int TOK_LIMIT = TOK_JOIN + 1;
  public static final int TOK_LUT = TOK_LIMIT + 1;
  public static final int TOK_MERGE = TOK_LUT + 1;
  public static final int TOK_MINUS = TOK_MERGE + 1;
  public static final int TOK_MOD = TOK_MINUS + 1;
  public static final int TOK_MUL = TOK_MOD + 1;
  public static final int TOK_NAME = TOK_MUL + 1;
  public static final int TOK_NULL = TOK_NAME + 1;
  public static final int TOK_NUMBER = TOK_NULL + 1;
  public static final int TOK_PLUS = TOK_NUMBER + 1;
  public static final int TOK_QUESTION = TOK_PLUS + 1;
  public static final int TOK_RETURN = TOK_QUESTION + 1;
  public static final int TOK_STATE = TOK_RETURN + 1;
  public static final int TOK_TEMPLATE = TOK_STATE + 1;
  public static final int TOK_TEMPL_CLOSE = TOK_TEMPLATE + 1;
  public static final int TOK_TEMPL_OPEN = TOK_TEMPL_CLOSE + 1;
  public static final int TOK_TEST = TOK_TEMPL_OPEN + 1;
  public static final int TOK_TYPE = TOK_TEST + 1;
  public static final int TOK_USE = TOK_TYPE + 1;
  public static final int UNKNONW_ID = -1;
  public static final int UNKNOWN_SYMBOL = -1;
  public final int colNr;
  public final int id;
  public final int lineNr;
  public final QuplaSource source;
  public final int symbol;
  public final String text;

  public Token(final int lineNr, final int colNr, final QuplaSource source, final int id, final int symbol, final String text)
  {
    this.colNr = colNr;
    this.id = id;
    this.lineNr = lineNr;
    this.source = source;
    this.symbol = symbol;
    this.text = text;
  }

  public Token resetId(final int tokenId)
  {
    return new Token(lineNr, colNr, source, tokenId, symbol, text);
  }

  public Token resetSymbol(final int symbol)
  {
    return new Token(lineNr, colNr, source, id, symbol, text);
  }

  public Token resetText(final String text)
  {
    return new Token(lineNr, colNr, source, id, symbol, text);
  }

  public Token resetText(final String text, final int tokenId, final int colNr)
  {
    return new Token(lineNr, colNr, source, tokenId, symbol, text);
  }

  @Override
  public String toString()
  {
    return "[" + (lineNr + 1) + "," + (colNr + 1) + "] " + id + " | " + text;
  }
}
