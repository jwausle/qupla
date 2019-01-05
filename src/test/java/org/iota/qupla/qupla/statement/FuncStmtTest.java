package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.expression.NameExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link FuncStmt}
 */
public class FuncStmtTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_func_then_nameExpected() {
        tokenizer.lines.add("func T name<T> (T val) {");
        tokenizer.lines.add("    return val");
        tokenizer.lines.add("}");
        tokenizer.nextToken();

        final FuncStmt underTest = new FuncStmt(tokenizer);
        Assert.assertEquals("name", underTest.name);

        Assert.assertEquals(1,underTest.funcTypes.size());
        Assert.assertEquals(ConstTypeName.class,underTest.funcTypes.get(0).getClass());
        Assert.assertEquals("T",underTest.funcTypes.get(0).name);

        Assert.assertEquals(1,underTest.params.size());
        Assert.assertEquals(NameExpr.class,underTest.params.get(0).getClass());
        Assert.assertEquals("T val",underTest.params.get(0).toString());

        Assert.assertEquals("val",underTest.returnExpr.name);
    }
}
