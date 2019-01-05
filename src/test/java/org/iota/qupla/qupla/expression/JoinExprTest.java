package org.iota.qupla.qupla.expression;

import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link JoinExpr}
 */
public class JoinExprTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_join_then_thisExpected() {
        tokenizer.lines.add("join this");
        tokenizer.nextToken();

        final JoinExpr underTest = new JoinExpr(tokenizer);
        Assert.assertEquals("this", underTest.name);
    }

    @Test
    public void when_join2_then_thisExpected() {
        tokenizer.lines.add("join this that");
        tokenizer.nextToken();

        final JoinExpr underTest = new JoinExpr(tokenizer);
        Assert.assertEquals("Is that right?", "this", underTest.name);
    }
}
