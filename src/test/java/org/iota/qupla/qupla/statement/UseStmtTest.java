package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link UseStmt}
 */
public class UseStmtTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_use_then_nameExpected() {
        tokenizer.lines.add("use name<Tryte>, <Tiny>, <Int>, <Huge>, <Hash>");
        tokenizer.nextToken();

        final UseStmt underTest = new UseStmt(tokenizer);
        Assert.assertEquals("name", underTest.name);

        Assert.assertEquals(1,underTest.typeArgs.size());
        Assert.assertEquals("Tryte",underTest.typeArgs.get(0).toString());
//        Assert.assertEquals("Tiny",underTest.typeInstantiations.get(1).get(0).toString());
//        Assert.assertEquals("Int",underTest.typeInstantiations.get(2).get(0).toString());
//        Assert.assertEquals("Huge",underTest.typeInstantiations.get(3).get(0).toString());
//        Assert.assertEquals("Hash",underTest.typeInstantiations.get(4).get(0).toString());
    }
}
