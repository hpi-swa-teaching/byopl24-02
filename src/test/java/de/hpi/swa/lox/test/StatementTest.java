package de.hpi.swa.lox.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class StatementTest extends AbstractLoxTest {
    @Test
    public void testExpressionStatement() {
        run("3 + 4;");
        assertThat(errContent.toString(), is(""));
    }

    @Test
    public void testBlockStatement() {
        run("{3 + 4;}");
        assertThat(errContent.toString(), is(""));
    }

    @Test
    public void testTwoStatement() {
        runAndExpect("two statements", "print 1; print 2;", "1\n2\n");
    }

    @Test
    public void testTwoStatementsWithNewline() {
        runAndExpect("two statements", "print 1;\n print 2;", "1\n2\n");
    }
}
