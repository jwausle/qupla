package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TemplateStmtTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule
            .class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_funcTemplate_then_nameExpected() {
        tokenizer.lines.add("template name<T> {");
        tokenizer.lines.add("  func T name<T> (T val) {");
        tokenizer.lines.add("    return val");
        tokenizer.lines.add("  }");
        tokenizer.lines.add("}");
        tokenizer.nextToken();

        final TemplateStmt underTest = new TemplateStmt(tokenizer);
        Assert.assertEquals("name", underTest.name);

        Assert.assertEquals(1,underTest.funcs.size());
        Assert.assertEquals("func T name<T>(T val)", underTest.funcs.get(0).toString());
    }
}
