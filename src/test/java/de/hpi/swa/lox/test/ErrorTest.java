package de.hpi.swa.lox.test;

import org.junit.Test;

public class ErrorTest extends AbstractLoxTest {

    @Test
    public void testParseError() {
        runAndExpectError("Print error", "print 1+;", "Error");
    }
}
