package org.iota.qupla.qupla.expression;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.ImportStmt;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link ImportStmt}
 */
public class ImportStmtTest {
    private final org.iota.qupla.qupla.parser.QuplaModule moduleMock = Mockito.mock(org.iota.qupla.qupla.parser.QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_importSingleStmt_then_moduleExpected() {
        tokenizer.lines.add("import Qupla");
        tokenizer.nextToken();

        final ImportStmt underTest = new ImportStmt(tokenizer);
        Assert.assertEquals("Qupla", underTest.name);
    }

    @Test
    public void when_importTwiceStmt_then_firstExpected() {
        tokenizer.lines.add("import Qupla Test");
        tokenizer.nextToken();

        final ImportStmt underTest = new ImportStmt(tokenizer);
        Assert.assertEquals("Qupla", underTest.name);
    }

    @Test
    public void when_importNoStmt_then_modulesExpected() {
        tokenizer.lines.add("import");
        tokenizer.nextToken();

        try {
            new ImportStmt(tokenizer);
            Assert.fail("Import without 'name' must fail");
        } catch (CodeException e) {
            Assert.assertEquals("Expected module name", e.getMessage());
            Assert.assertNotNull("Expect exception token", e.token);
            Assert.assertEquals("Expect EOF token", Token.TOK_EOF, e.token.id);
        }
    }
}
