
/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.hpi.swa.lox.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PrintTest extends AbstractLoxTest {
    @Test
    public void printTrue() {
        runAndExpect("printTestOutput", "print true;", "true\n");
    }

    @Test
    public void printFalse() {
        runAndExpect("printTestOutput", "print false;", "false\n");
    }

    @Test
    public void printString() {
        runAndExpect("printTestOutput", "print \"hello\";", "hello\n");
    }

    @Test
    public void printBoolean_inverted() {
        runAndExpect("printTestOutput", "print !false;", "true\n");
        runAndExpect("printTestOutput", "print !true;", "false\n");
    }

    @Test
    public void printNil() {
        runAndExpect("printTestOutput", "print nil;", "nil\n");
    }

    @Test
    public void printFloatingNumber() {
        runAndExpect("printTestOutput", "print 12.34;", "12.34\n");
    }

    @Test
    public void printFloatingNumber_negated() {
        runAndExpect("printTestOutput", "print -12.34;", "-12.34\n");
    }

    @Test
    public void printFloatingNumber_withoutDigitsBeforeDecimalPoint() {
        run("print .12;");
        assertTrue(outContent.toString().contains("Error(s) parsing script"));
    }

    @Test
    public void printFloatingNumber_withoutDigitsAfterDecimalPoint() {
        run("print 12.;");
        assertTrue(outContent.toString().contains("Error(s) parsing script"));
    }

    @Test
    public void printIntegerNumber() {
        runAndExpect("printTestOutput", "print 12;", "12\n");
    }

    @Test
    public void printIntegerNumber_negated() {
        runAndExpect("printTestOutput", "print -12;", "-12\n");
    }

    @Test
    public void printIntegerNumber_added() {
        runAndExpect("printTestOutput", "print 12 + 34;", "46\n");
    }

    @Test
    public void printIntegerNumber_added_negated() {
        runAndExpect("printTestOutput", "print -12 + 34;", "22\n");
    }

    @Test
    public void printIntegerNumber_subtracted() {
        runAndExpect("printTestOutput", "print 12 - 34;", "-22\n");
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
    public void printIntegerNumber_multiplied_added() {
        runAndExpect("printTestOutput", "print 12 * 34 + 56 * 4 * 7;", "1976\n");
    }

    @Test
    public void printIntegerNumber_divided() {
        runAndExpect("printTestOutput", "print 12 / 34;", "0.35294117647058826\n");
    }

    @Test
    public void printIntegerNumber_divided_null() {
        boolean exceptionThrown = false;
        try {
            run("print 12 / 0;");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Division by zero"));
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void printIntegerNumber_complex() {
        runAndExpect("printTestOutput", "print 12 * (34 + 56) / 4 - 7;", "263\n");
    }

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
    public void printLogicalOrComparison_mutiple_booleans() {
        runAndExpect("printTestOutput", "print true or false or true;", "true\n");
    }
    
    @Test
    public void printLogicalOrComparison_not_matching_number_left() {
        try {
            run("print 4 or true");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply logical_or"));
        }
    }

    @Test
    public void printLogicalOrComparison_not_matching_number_right() {
        try {
            run("print true or 4");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply logical_or"));
        }
    }

    @Test
    public void printLogicalOrComparison_not_matching_types() {
        try {
            run("print true or \"test\"");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply logical_or"));
        }
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
    public void printLogicalAndComparison_not_matching_number_left() {
        try {
            run("print 4 and true");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply logical_and"));
        }
    }

    @Test
    public void printLogicalAndComparison_not_matching_number_right() {
        try {
            run("print true and 4");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply logical_and"));
        }
    }

    @Test
    public void printLogicalAndComparison_not_matching_types() {
        try {
            run("print true and \"test\"");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply logical_and"));
        }
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
        try {
            run("print true < \"test\"");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply <"));
        }
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
        try {
            run("print true <= \"test\"");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply <="));
        }
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
        try {
            run("print true > \"test\"");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply >"));
        }
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
        try {
            run("print true >= \"test\"");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot apply >="));
        }
    }

}
