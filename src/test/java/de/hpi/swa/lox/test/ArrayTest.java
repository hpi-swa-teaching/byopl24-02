package de.hpi.swa.lox.test;

import org.junit.Test;

public class ArrayTest extends AbstractLoxTest {

    @Test
    public void testNewArray() {
        runAndExpect("new array", "var a = 🪹; print a;", "🪹\n");
    }

    @Test
    public void testGetNil() {
        runAndExpect("get nil", "var a = 🪹; print a🫲1🫱;",
                "nil\n");
    }

    @Test
    public void testSetArray() {
        runAndExpect("set a🫲0🫱=3", "var a = 🪹; a🫲0🫱=3; print a🫲0🫱;", "3\n");
    }

    @Test
    public void testPrintArray() {
        runAndExpect("print contents",
                "var a = 🪹; a🫲0🫱=3; a🫲1🫱=\"hello\"; print a;", "🫲3, hello🫱\n");
    }

}
