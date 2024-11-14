package de.hpi.swa.lox.test;

import org.junit.Test;

public class VariablesTest extends AbstractLoxTest {

    @Test
    public void testWriteAndReadVariable() {
        runAndExpect("read write and read variable", "var a = 3; print a;", "3\n");
    }

    @Test
    public void testBlockWriteAndReadVariable() {
        runAndExpect("read write and read variable", "{var a = 4; print a;}", "4\n");
    }

    @Test
    public void testShadowedWriteAndReadVariable() {
        runAndExpect("read write and read variable", "var a = 3; print a; {var a = 4; print a;} print a;", "3\n4\n3\n");
    }

    @Test
    public void testReadUndefinedLocalVariable() {
        runAndExpectError("read undefined local variable", "{var a; print a;}", "not defined");
    }

    @Test
    public void testReadUndeclaredLocalVariable() {
        runAndExpectError("read undefined local variable", "{print a;}", "not declared");
    }

    @Test
    public void testReadUndefinedGlobalVariable() {
        runAndExpectError("read undefined global variable", "var a; {print a;}", "not defined");
    }
}
