package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PostfixExprTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_1_then_int1_expected() {
        tokenizer.lines.add("1");
        tokenizer.nextToken();

        final PostfixExpr underTest = new PostfixExpr(tokenizer);
        Assert.assertEquals(null, underTest.name);
        Assert.assertEquals("1",underTest.expr.name);
    }

    @Test
    public void when_minus1dot1_then_intMinus1dot1_expected() {
        tokenizer.lines.add("-1.1");
        tokenizer.nextToken();

        final PostfixExpr underTest = new PostfixExpr(tokenizer);
        Assert.assertEquals(null, underTest.name);
        Assert.assertEquals("-1.1",underTest.expr.name);
    }
}
