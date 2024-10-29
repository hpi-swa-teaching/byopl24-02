package de.hpi.swa.lox.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ErrorTest extends AbstractLoxTest {

    @Test
    public void testParseError() {
        run("print 1 +");
        assertTrue(outContent.toString().contains("Error"));
    }
}
