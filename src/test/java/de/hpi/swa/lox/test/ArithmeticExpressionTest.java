package de.hpi.swa.lox.test;

import org.junit.Test;

public class ArithmeticExpressionTest extends AbstractLoxTest {

    @Test
    public void printIntegerNumber_negated() {
        runAndExpect("Integer number negated", "print -12;", "-12\n");
    }

    @Test
    public void printFloatingNumber_negated() {
        runAndExpect("Floating number negated", "print -12.34;", "-12.34\n");
    }

    @Test
    public void printIntegerNumber_negated_not_matching_type() {
        runAndExpectError("Integer number negated not matching type", "print -true;", "Cannot negate");
    }

    @Test
    public void printIntegerNumber_added() {
        runAndExpect("Integer Number add", "print 12 + 34;", "46\n");
        runAndExpect("Integer Number add and subtract", "print -12 + 34;", "22\n");
    }

    @Test
    public void printIntegerNumber_added_not_matching_types() {
        runAndExpectError("Integer number add not matching type right", "print 12 + \"test\";",
                "Cannot add");
        runAndExpectError("Integer number add not matching type left", "print \"test\" + 12;",
                "Cannot add");
        runAndExpectError("Integer number add not matching type", "print \"test\" + \"test\";",
                "Cannot add");
    }

    @Test
    public void printIntegerNumber_subtracted() {
        runAndExpect("printTestOutput", "print 12 - 34;", "-22\n");
    }

    @Test
    public void printIntegerNumber_subtracted_not_matching_types() {
        runAndExpectError("Integer number subtract not matching type right", "print 12 - \"test\";",
                "Cannot subtract");
        runAndExpectError("Integer number subtract not matching type left", "print \"test\" - 12;",
                "Cannot subtract");
        runAndExpectError("Integer number subtract not matching type", "print \"test\" - \"test\";",
                "Cannot subtract");
    }

    @Test
    public void printIntegerNumber_subtracted_negated() {
        runAndExpect("printTestOutput", "print -12 - 34;", "-46\n");
    }

    @Test
    public void printIntegerNumber_subtracted_added() {
        runAndExpect("printTestOutput", "print 12 - 34 + 56 - 4 - 7;", "23\n");
    }

    @Test
    public void printIntegerNumber_added_subtracted_negated() {
        runAndExpect("printTestOutput", "print (12 + 34) - (56 + 4 - 7);", "-7\n");
    }

    @Test
    public void printIntegerNumber_multiplied() {
        runAndExpect("printTestOutput", "print 12 * 34;", "408\n");
    }

    @Test
    public void printIntegerNumber_multiplied_not_matching_types() {
        runAndExpectError("Integer number multiplied not matching type right", "print 12 * \"test\";",
                "Cannot multiply");
        runAndExpectError("Integer number multiplied not matching type left", "print \"test\" * 12;",
                "Cannot multiply");
        runAndExpectError("Integer number multiplied not matching type", "print \"test\" * \"test\";",
                "Cannot multiply");
    }

    @Test
    public void printIntegerNumber_multiplied_added() {
        runAndExpect("printTestOutput", "print 12 * 34 + 56 * 4 * 7;", "1976\n");
    }

    @Test
    public void printIntegerNumber_divided() {
        runAndExpect("printTestOutput", "print 12 / 34;", "0.35294117647058826\n");
    }

    @Test
    public void printIntegerNumber_divided_null() {
        runAndExpectError("Integer Number divided by null", "print 12 / 0;", "Division by zero");
    }

    @Test
    public void printIntegerNumber_divided_not_matching_types() {
        runAndExpectError("Integer number divided not matching types right", "print 12 / \"test\";",
                "Cannot divide");
        runAndExpectError("Integer number divided not matching types left", "print \"test\" / 2;",
                "Cannot divide");
        runAndExpectError("Integer number divided not matching types", "print \"test\" / \"test\";",
                "Cannot divide");
    }

    @Test
    public void printIntegerNumber_complex() {
        runAndExpect("printTestOutput", "print 12 * (34 + 56) / 4 - 7;", "263\n");
    }
}
