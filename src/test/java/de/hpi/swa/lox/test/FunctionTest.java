package de.hpi.swa.lox.test;

import org.junit.Test;

public class FunctionTest extends AbstractLoxTest {

    @Test
    public void testFunctionDeclaration() {
        runAndExpect("function declaration without call to body", "print 2;fun f() { print 1; }; print 3;", "2\n3\n");
    }

    @Test
    public void testFunctionCall() {
        runAndExpect("function calls", "fun f() { print 1; } f();f();", "1\n1\n");
    }

    @Test
    public void testFunctionAsValues() {
        runAndExpect("function expression", "fun x() { print 1; }; var f = x; f();", "1\n");
    }

    @Test
    public void testFunctionDeclarationWithParameter() {
        runAndExpect("function declaration with parameter", "fun f(a) { print a; } f(1);", "1\n");
    }

    @Test
    public void testFunctionReturn() {
        runAndExpect("function return", "fun f() { return 1; } print f();", "1\n");
    }

    @Test
    public void testInnerFunction() {
        runAndExpect("inner function",
                "fun outer() { " +
                        " fun inner() { " +
                        " return 1;" +
                        " }" +
                        " return inner();" +
                        "}" +
                        "print outer();",
                "1\n");
    }

    @Test
    public void testInnerFunctionReadsOuterVariable() {
        runAndExpect("inner function uses outer variable",
                "fun outer() { " +
                        " var a = 1;" +
                        " fun inner() { " +
                        " return a;" +
                        " }" +
                        " return inner();" +
                        "}" +
                        "print outer();",
                "1\n");
    }

    @Test
    public void testInnerFunctionStoreOuterVariable() {
        runAndExpect("inner function uses outer variable",
                "fun outer() { " +
                        " var a = 1;" +
                        " fun inner() { " +
                        " a = 2;" +
                        " return a;" +
                        " }" +
                        " return inner();" +
                        "}" +
                        "print outer();",
                "2\n");
    }

    @Test
    public void testFunctionReturnsFunction() {
        runAndExpect("function returns function",
                "fun outer() {" +
                        " fun inner() {" +
                        " return 1;" +
                        " }" +
                        " return inner;" +
                        "}" +
                        "print outer()();",
                "1\n");
    }
}
