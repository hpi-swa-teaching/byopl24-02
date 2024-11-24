package de.hpi.swa.lox.test;

import org.junit.Test;

public class ControlFlowTest extends AbstractLoxTest {
    @Test
    public void testForLoop() {
        runAndExpect("print 1 2 3", "for(var i=1; i <= 3; i = i + 1) {print i;}", "1\n2\n3\n");
    }

    @Test
    public void testForNonVarInitLoop() {
        runAndExpect("print 1 2 3",
                "var i; for(i=1; i <= 3; i = i + 1) {print i;}", "1\n2\n3\n");
    }

    @Test
    public void testForOfLoop_through_array() {
        runAndExpect("print every element of array with for of", "var a = 👉1, 2, 3👈; for(var element of a) {print element;}", "1\n2\n3\n");
    }

    @Test
    public void testForOfLoop_through_number() {
        runAndExpectError("for of with number as toIterate", "var a = 4; for(var element of a) {print element;}", "is not an LoxArray");
    }

    @Test
    public void testForInLoop_through_array() {
        runAndExpect("print every index of array with for in", "var a = 👉1, 2, 3👈; for(var index in a) {print index;}", "0\n1\n2\n");
    }

    @Test
    public void testForInLoop_through_number() {
        runAndExpectError("for in with number as toIterate", "var a = 4; for(var index in a) {print index;}", "is not an LoxArray");
    }

    @Test
    public void testWhileLoop() {
        runAndExpect("print 1 2 3",
                "var i=0; while(i < 3) { i = i + 1; print i;}", "1\n2\n3\n");
    }

    @Test
    public void testIfCondition() {
        runAndExpect("if true", "if(true) print 1;", "1\n");
        runAndExpect("if true", "if(true) { print 1;}", "1\n");
        runAndExpect("if false", "if(false) print 1;", "");
    }

    @Test 
    public void testIfElseCondition() {
        runAndExpect("if true","if(true) print 1; else print 2;", "1\n");
        runAndExpect("if false","if(false) print 1; else print 2;", "2\n");
    }

}
