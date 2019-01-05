package org.iota.qupla.qupla.statement;

import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link TypeStmt}
 */
public class TypeStmtTest {
    private final QuplaModule moduleMock = Mockito.mock(QuplaModule.class);
    private final Tokenizer tokenizer = new Tokenizer();

    @Before
    public void setup() {
        tokenizer.module = moduleMock;
    }

    @Test
    public void when_type_then_typeNameExpected() {
        tokenizer.lines.add("type TypeName [TritVector]");
        tokenizer.nextToken();

        final TypeStmt underTest = new TypeStmt(tokenizer);
        Assert.assertEquals("TypeName", underTest.name);
        Assert.assertNotNull(underTest.vector);
        Assert.assertEquals("TypeName [TritVector]", underTest.vector.toString());
        Assert.assertEquals(ConstTypeName.class, underTest.vector.typeExpr.getClass());
    }
}
