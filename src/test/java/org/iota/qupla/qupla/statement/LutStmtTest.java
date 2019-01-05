package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link LutStmt}
 */
public class LutStmtTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_lut_then_nameExpected() {
        tokenizer.lines.add("lut name {");
        tokenizer.lines.add("    -,- = 1");
        tokenizer.lines.add("    -,0 = 0");
        tokenizer.lines.add("    -,1 = -");
        tokenizer.lines.add("    0,- = 1");
        tokenizer.lines.add("    0,0 = -");
        tokenizer.lines.add("    0,1 = 0");
        tokenizer.lines.add("    1,- = -");
        tokenizer.lines.add("    1,0 = 1");
        tokenizer.lines.add("    1,1 = 0");
        tokenizer.lines.add("}");
        tokenizer.nextToken();

        final LutStmt underTest = new LutStmt(tokenizer);
        Assert.assertEquals("name", underTest.name);
        Assert.assertEquals(9, underTest.entries.size());
        Assert.assertEquals("-,- = 1", underTest.entries.get(0).toString());
        Assert.assertEquals("-,0 = 0", underTest.entries.get(1).toString());
        Assert.assertEquals("-,1 = -", underTest.entries.get(2).toString());
        Assert.assertEquals("0,- = 1", underTest.entries.get(3).toString());
        Assert.assertEquals("0,0 = -", underTest.entries.get(4).toString());
        Assert.assertEquals("0,1 = 0", underTest.entries.get(5).toString());
        Assert.assertEquals("1,- = -", underTest.entries.get(6).toString());
        Assert.assertEquals("1,0 = 1", underTest.entries.get(7).toString());
        Assert.assertEquals("1,1 = 0", underTest.entries.get(8).toString());
    }
}
