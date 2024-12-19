package de.hpi.swa.lox.test;

import org.junit.Test;

public class LogicalExpressionTest extends AbstractLoxTest {
    @Test
    public void printLogicalOrComparison_two_booleans() {
        runAndExpect("printTestOutput", "print true or false;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_one_boolean_and_0_left() {
        runAndExpect("printTestOutput", "print 0 or true;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_one_boolean_and_0_right() {
        runAndExpect("printTestOutput", "print true or 0;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_one_boolean_and_1_left() {
        runAndExpect("printTestOutput", "print 1 or false;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_one_boolean_and_1_right() {
        runAndExpect("printTestOutput", "print false or 1;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_two_1() {
        runAndExpect("printTestOutput", "print 1 or 1;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_1_left_0_right() {
        runAndExpect("printTestOutput", "print 1 or 0;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_0_left_1_right() {
        runAndExpect("printTestOutput", "print 0 or 1;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_two_0() {
        runAndExpect("printTestOutput", "print 0 or 0;", "false\n");
    }

    @Test
    public void printLogicalOrComparison_multiple_booleans() {
        runAndExpect("printTestOutput", "print true or false or true;", "true\n");
    }

    @Test
    public void printLogicalOrComparison_non_binary_number_left() {
        runAndExpect("Logical or comparison not matching number left", "print 4 or true;",
                "true\n");
    }

    @Test
    public void printLogicalOrComparison_non_binary_number_right() {
        runAndExpect("Logical or comparison non binary number right", "print true or 4;",
                "true\n");
    }

    @Test
    public void printLogicalOrComparison_string_types() {
        runAndExpect("Logical or comparison string type", "print true or \"test\";",
                "true\n");
    }

    @Test
    public void printLogicalAndComparison_two_booleans() {
        runAndExpect("printTestOutput", "print true and false;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_one_boolean_and_0_left() {
        runAndExpect("printTestOutput", "print 0 and true;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_one_boolean_and_0_right() {
        runAndExpect("printTestOutput", "print true and 0;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_one_boolean_and_1_left() {
        runAndExpect("printTestOutput", "print 1 and false;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_one_boolean_and_1_right() {
        runAndExpect("printTestOutput", "print false and 1;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_two_1() {
        runAndExpect("printTestOutput", "print 1 and 1;", "true\n");
    }

    @Test
    public void printLogicalAndComparison_1_left_0_right() {
        runAndExpect("printTestOutput", "print 1 and 0;", "false\n");
    }

    @Test
    public void printLogicalOAndComparison_0_left_1_right() {
        runAndExpect("printTestOutput", "print 0 and 1;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_two_0() {
        runAndExpect("printTestOutput", "print 0 and 0;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_mutiple_booleans() {
        runAndExpect("printTestOutput", "print true and false and true;", "false\n");
    }

    @Test
    public void printLogicalAndComparison_no_binary_number_left() {
        runAndExpect("Logical And comparison non binary number left", "print 4 and true;",
                "true\n");
    }

    @Test
    public void printLogicalAndComparison_non_binary_number_right() {
        runAndExpect("Logical And comparison non binary number right", "print true and 4;",
                "true\n");
    }

    @Test
    public void printLogicalAndComparison_string_types() {
        runAndExpect("Logical and comparison string types", "print true and \"test\";",
                "true\n");
    }

    @Test
    public void printEqualComparison_two_booleans() {
        runAndExpect("printTestOutput", "print true == false;", "false\n");
    }

    @Test
    public void printEqualComparison_two_numbers() {
        runAndExpect("printTestOutput", "print 2 == 2;", "true\n");
    }

    @Test
    public void printEqualComparison_mutiple_values() {
        runAndExpect("printTestOutput", "print true == false == true;", "false\n");
    }

    @Test
    public void printInEqualComparison_two_booleans() {
        runAndExpect("printTestOutput", "print true != false;", "true\n");
    }

    @Test
    public void printInEqualComparison_two_numbers() {
        runAndExpect("printTestOutput", "print 2 != 2;", "false\n");
    }

    @Test
    public void printInEqualComparison_mutiple_values() {
        runAndExpect("printTestOutput", "print true != true != true;", "true\n");
    }

    @Test
    public void printLessComparison_two_equal_numbers() {
        runAndExpect("printTestOutput", "print 2 < 2;", "false\n");
    }

    @Test
    public void printLessComparison_two_numbers_less_left() {
        runAndExpect("printTestOutput", "print 2 < 3;", "true\n");
    }

    @Test
    public void printLessComparison_two_numbers_less_right() {
        runAndExpect("printTestOutput", "print 3 < 2;", "false\n");
    }

    @Test
    public void printLessComparison_not_matching_types() {
        runAndExpectError("Less Comparison not matching types", "print true < \"test\";", "Cannot apply <");
    }

    @Test
    public void printLessOrEqualComparison_two_equal_numbers() {
        runAndExpect("printTestOutput", "print 2 <= 2;", "true\n");
    }

    @Test
    public void printLessOrEqualComparison_two_numbers_less_left() {
        runAndExpect("printTestOutput", "print 2 <= 3;", "true\n");
    }

    @Test
    public void printLessOrEqualComparison_two_numbers_less_right() {
        runAndExpect("printTestOutput", "print 3 <= 2;", "false\n");
    }

    @Test
    public void printLessOrEqualComparison_not_matching_types() {
        runAndExpectError("Less or equal comparison not matching types", "print true <= \"test\";", "Cannot apply <=");
    }

    @Test
    public void printGreaterComparison_two_equal_numbers() {
        runAndExpect("printTestOutput", "print 2 > 2;", "false\n");
    }

    @Test
    public void printGreaterComparison_two_numbers_less_left() {
        runAndExpect("printTestOutput", "print 2 > 3;", "false\n");
    }

    @Test
    public void printGreaterComparison_two_numbers_less_right() {
        runAndExpect("printTestOutput", "print 3 > 2;", "true\n");
    }

    @Test
    public void printGreaterComparison_not_matching_types() {
        runAndExpectError("Greater comparison not matching type right", "print true > \"test\";", "Cannot apply >");
        runAndExpectError("Greater comparison not matching type left", "print \"test\" > true;", "Cannot apply >");
        runAndExpectError("Greater comparison not matching type", "print \"test\" > \"test\";", "Cannot apply >");
    }

    @Test
    public void printGreaterOrEqualComparison_two_equal_numbers() {
        runAndExpect("printTestOutput", "print 2 >= 2;", "true\n");
    }

    @Test
    public void printGreaterOrEqualComparison_two_numbers_less_left() {
        runAndExpect("printTestOutput", "print 2 >= 3;", "false\n");
    }

    @Test
    public void printGreaterOrEqualComparison_two_numbers_less_right() {
        runAndExpect("printTestOutput", "print 3 >= 2;", "true\n");
    }

    @Test
    public void printGreaterOrEqualComparison_not_matching_types() {
        runAndExpectError("Greater or equal comparison not matching types", "print true >= \"test\";",
                "Cannot apply >=");
    }

    @Test
    public void testShortCircuitedLogicalOperators() {
        runAndExpect("init", "fun a() { print \"a\"; return true;} fun b() { print \"b\"; return false;}", "");
        runAndExpect("a and b", "a() and b(); ", "a\nb\n");
        runAndExpect("b and a", " b() and a(); ", "b\n");
        runAndExpect("a or b", "a() or b(); ", "a\n");
        runAndExpect("b or a", "b() or a(); ", "b\na\n");
    }
}
