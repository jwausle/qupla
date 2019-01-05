package org.iota.qupla.qupla.expression;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AffectExprTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_affectFirst_then_firstExpected() {
        tokenizer.lines.add("affect first");
        tokenizer.nextToken();

        final AffectExpr underTest = new AffectExpr(tokenizer);
        Assert.assertEquals("affect",underTest.origin.text);
        Assert.assertEquals("first", underTest.name);
    }

    @Test
    public void when_affectSecond_then_secondExpected() {
        tokenizer.lines.add("affect second");
        tokenizer.nextToken();

        final AffectExpr underTest = new AffectExpr(tokenizer);
        Assert.assertEquals("affect",underTest.origin.text);
        Assert.assertEquals("second", underTest.name);
    }

    @Test
    public void when_affectFirstSecond_then_firstExpected() {
        tokenizer.lines.add("affect first second");
        tokenizer.nextToken();

        final AffectExpr underTest = new AffectExpr(tokenizer);
        Assert.assertEquals("affect",underTest.origin.text);
        Assert.assertEquals("first", underTest.name);
    }

    @Test
    public void when_affectExpr_withoutEnvironment_Then_exceptionExpected() {
        tokenizer.lines.add("affect");
        tokenizer.nextToken();

        try {
            new AffectExpr(tokenizer);
            Assert.fail("Affect without 'environment' must fail");
        } catch (CodeException e) {
            Assert.assertEquals("Expected environment name", e.getMessage());
            Assert.assertNotNull("Expect exception token", e.token);
            Assert.assertEquals("Expect EOF token", Token.TOK_EOF, e.token.id);
        }
    }
}
