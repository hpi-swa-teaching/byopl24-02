package de.hpi.swa.lox.test;

import org.junit.Test;

public class VariablesTest extends AbstractLoxTest {
    
    @Test
    public void testWriteAndReadVariable() {
        runAndExpect("read write and read variable", "var a = 3; print a;", "3\n");
    }
}
