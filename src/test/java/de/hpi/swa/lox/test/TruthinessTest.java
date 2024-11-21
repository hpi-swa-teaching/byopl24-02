package de.hpi.swa.lox.test;

import org.junit.Test;

public class TruthinessTest extends AbstractLoxTest {
    @Test
    public void testTruthiness() {
        runAndExpect("testNilIsFalse", "print !nil;", "true\n");
    }

    @Test
    public void testTruthinessCondition() {
        runAndExpect("testNumbersAreTrue", "if(1) print 3;", "3\n");
        runAndExpect("testStringsAreTrue", "if(\"hello\") print 3;", "3\n");
        runAndExpect("testNilIsFalseInCondtion", "if(nil) print 3;", "");
        runAndExpect("testNilIsFalseInCondtion", "if(nil) print 3; else print 4;", "4\n");

    }

    @Test
    public void testTruthinessWhile() {
        runAndExpect("testNumbersAreTrue",
                "var a = true; while(a) { print a; a = nil;}",
                "true\n");
    }

    @Test
    public void testTruthinessFor() {
        runAndExpect("testNumbersAreTrue",
                "for(var a=true; a ; a = nil) { print a;}",
                "true\n");
    }
}
