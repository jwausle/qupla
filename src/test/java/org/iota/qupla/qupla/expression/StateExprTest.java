package org.iota.qupla.qupla.expression;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link StateExpr}
 */
public class StateExprTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_stateExpr_then_typeWihtVariableExpected() {
        tokenizer.lines.add("state V value");
        tokenizer.nextToken();

        final StateExpr underTest = new StateExpr(tokenizer);
        Assert.assertEquals("state",underTest.origin.text);
        Assert.assertEquals(ConstTypeName.class, underTest.stateType.getClass());
        Assert.assertEquals("V", underTest.stateType.name);
        Assert.assertEquals("value", underTest.name);
    }

    @Test
    public void when_stateExpr_withoutVariable_Then_exceptionExpected() {
        tokenizer.lines.add("state V");
        tokenizer.nextToken();

        try {
            new StateExpr(tokenizer);
            Assert.fail("State without 'variable' must fail");
        } catch (CodeException e) {
            Assert.assertEquals("Expected variable name", e.getMessage());
            Assert.assertNotNull("Expect exception token", e.token);
            Assert.assertEquals("Expect EOF token", Token.TOK_EOF, e.token.id);
        }
    }
}
