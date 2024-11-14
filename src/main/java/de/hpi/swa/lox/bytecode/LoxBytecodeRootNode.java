package de.hpi.swa.lox.bytecode;

import java.io.IOException;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.nodes.LoxRootNode;
import de.hpi.swa.lox.runtime.LoxContext;
import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.LoxNumber;

@GenerateBytecode(//
        languageClass = LoxLanguage.class, //
        boxingEliminationTypes = { long.class, boolean.class }, //
        enableUncachedInterpreter = true, //
        enableSerialization = true)
public abstract class LoxBytecodeRootNode extends LoxRootNode implements BytecodeRootNode {

    protected LoxBytecodeRootNode(LoxLanguage language, FrameDescriptor frameDescriptor) {
        super(language, frameDescriptor);
    }

    @Operation
    public static final class LoxPrint {
        @Specialization
        @TruffleBoundary
        static void doDefault(Object value, @Bind LoxContext context) {
            var out = context.getOutput();
            try {
                out.write(Objects.toString(value).getBytes());
                out.write(System.lineSeparator().getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Operation
    public static final class LoxInvert {
        @Specialization
        static boolean doBoolean(boolean value) {
            return !value;
        }

        @Fallback
        @TruffleBoundary
        static boolean doOtherTypes(Object value, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot invert %s", value), node);
        }
    }

    @Operation
    public static final class LoxNegate {
        @Specialization
        static LoxNumber doNumber(LoxNumber loxNumber) {
            // Unwrap LoxNumber value, calculate result, rewrap.
            Double result = -1 * loxNumber.getValue();
            return new LoxNumber(result);
        }

        @Fallback
        @TruffleBoundary
        static LoxNumber doOtherTypes(Object value, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot negate %s", value), node);
        }
    }

    @Operation
    public static final class LoxAdd {
        @Specialization
        static LoxNumber doNumbers(LoxNumber left, LoxNumber right) {
            // Unwrap LoxNumber values, calculate result, rewrap.
            Double result = left.getValue() + right.getValue();
            return new LoxNumber(result);
        }

        @Specialization
        @TruffleBoundary
        static LoxNumber doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot add %s and %s", left.toString(), right.toString()), node);
        }
    }

    @Operation
    public static final class LoxSubtract {
        @Specialization
        static LoxNumber doNumber(LoxNumber left, LoxNumber right) {
            // Unwrap LoxNumber values, calculate result, rewrap.
            Double result = left.getValue() - right.getValue();
            return new LoxNumber(result);
        }

        @Fallback
        @TruffleBoundary
        static LoxNumber doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot subtract %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxMultiply {
        @Specialization
        static LoxNumber doNumbers(LoxNumber left, LoxNumber right) {
            // Unwrap LoxNumber values, calculate result, rewrap.
            Double result = left.getValue() * right.getValue();
            return new LoxNumber(result);
        }

        @Fallback
        @TruffleBoundary
        static LoxNumber doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot multiply %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxDivide {
        @Specialization
        static LoxNumber doNumbers(LoxNumber left, LoxNumber right, @Bind Node node) {
            if (right.getValue() == 0) {
                throw new LoxRuntimeError("Division by zero", node);
            }
            // Unwrap LoxNumber values, calculate result, rewrap.
            Double result = left.getValue() / right.getValue();

            return new LoxNumber(result);
        }

        @Fallback
        @TruffleBoundary
        static LoxNumber doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot divide %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxOr {
        @Specialization
        static boolean doBoolean(boolean left, boolean right) {
            return left || right;
        }

        @Specialization
        static boolean doBooleanAndLoxNumber(boolean left, LoxNumber right, @Bind Node node) {
            if (right.getValue() == 0) {
                // 0 intepreted as false, so only left side matters.
                return left;
            } else if (right.getValue() == 1) {
                // 1 intepreted as true, so or expression is instantly true.
                return true;
            }
            // Other numbers -> RuntimeError
            throw createRuntimeError(left, right, node);
        }

        @Specialization
        static boolean doLoxNumberAndBoolean(LoxNumber left, boolean right, @Bind Node node) {
            if (left.getValue() == 0) {
                // 0 intepreted as false, so only right side matters.
                return right;
            } else if (left.getValue() == 1) {
                // 1 intepreted as true, so or expression is instantly true.
                return true;
            }
            // Other numbers -> RuntimeError
            throw createRuntimeError(left, right, node);
        }

        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right, @Bind Node node) {
            if (left.getValue() == 0 && right.getValue() == 0) {
                // 0 intepreted as false
                return false;
            } else if (left.getValue() == 0 && right.getValue() == 1) {
                // 1 intepreted as true, so or expression is instantly true.
                return true;
            } else if (left.getValue() == 1 && right.getValue() == 0) {
                // 1 intepreted as true, so or expression is instantly true.
                return true;
            } else if (left.getValue() == 1 && right.getValue() == 1) {
                // 1 intepreted as true, so or expression is instantly true.
                return true;
            }
            // Other numbers -> RuntimeError
            throw createRuntimeError(left, right, node);
        }

        @Fallback
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw createRuntimeError(left, right, node);
        }

        @TruffleBoundary
        static LoxRuntimeError createRuntimeError(Object left, Object right, Node node) {
            return new LoxRuntimeError(
                    String.format("Cannot apply logical_or on %s and %s", left.toString(), right.toString()), node);
        }
    }

    @Operation
    public static final class LoxAnd {
        @Specialization
        static boolean doBoolean(boolean left, boolean right) {
            return left && right;
        }

        @Specialization
        static boolean doBooleanAndLoxNumber(boolean left, LoxNumber right, @Bind Node node) {
            if (right.getValue() == 0) {
                // 0 intepreted as false, so and expression is instantly false.
                return false;
            } else if (right.getValue() == 1) {
                // 1 intepreted as true, so left side matters for and expression.
                return left;
            }
            // Other numbers -> RuntimeError
            throw createRuntimeError(left, right, node);
        }

        @Specialization
        static boolean doLoxNumberAndBoolean(LoxNumber left, boolean right, @Bind Node node) {
            if (left.getValue() == 0) {
                // 0 intepreted as false, so and expression is instantly false.
                return false;
            } else if (left.getValue() == 1) {
                // 1 intepreted as true, so right side matters for and expression.
                return right;
            }
            // Other numbers -> RuntimeError
            throw createRuntimeError(left, right, node);
        }

        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right, @Bind Node node) {
            if (left.getValue() == 0 && right.getValue() == 0) {
                // 0 intepreted as false, so and expression is instantly false.
                return false;
            } else if (left.getValue() == 0 && right.getValue() == 1) {
                // 0 intepreted as false, so and expression is instantly false.
                return false;
            } else if (left.getValue() == 1 && right.getValue() == 0) {
                // 0 intepreted as false, so and expression is instantly false.
                return false;
            } else if (left.getValue() == 1 && right.getValue() == 1) {
                // 1 intepreted as true, so and expression is instantly true.
                return true;
            }
            // Other numbers -> RuntimeError
            throw createRuntimeError(left, right, node);
        }

        @Fallback
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw createRuntimeError(left, right, node);
        }

        @TruffleBoundary
        static LoxRuntimeError createRuntimeError(Object left, Object right, Node node) {
            return new LoxRuntimeError(
                    String.format("Cannot apply logical_and on %s and %s", left.toString(), right.toString()), node);
        }
    }

    @Operation
    public static final class LoxInequal {
        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
            return !left.equals(right);
        }

        @Specialization
        static boolean doDefault(Object left, Object right) {
            return left != right;
        }
    }

    @Operation
    public static final class LoxEqual {
        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
            return left.equals(right);
        }

        @Specialization
        static boolean doDefault(Object left, Object right) {
            return left == right;
        }
    }

    @Operation
    public static final class LoxLess {
        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
            if (left.equals(right)) {
                // Remember: we internally deal with doubles, that might be unequal only a
                // little bit.
                return false;
            }
            return left.getValue() < right.getValue();
        }

        @Fallback
        @TruffleBoundary
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot apply < on %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxLessOrEqual {
        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
            if (left.equals(right)) {
                // Remember: we internally deal with doubles, that might be unequal only a
                // little bit.
                return true;
            }
            return left.getValue() <= right.getValue();
        }

        @Fallback
        @TruffleBoundary
        static Object doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot apply <= on %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxGreater {
        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
            if (left.equals(right)) {
                // Remember: we internally deal with doubles, that might be unequal only a
                // little bit.
                return false;
            }
            return left.getValue() > right.getValue();
        }

        @Fallback
        @TruffleBoundary
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot apply > on %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxGreaterOrEqual {
        @Specialization
        static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
            if (left.equals(right)) {
                // Remember: we internally deal with doubles, that might be unequal only a
                // little bit.
                return true;
            }
            return left.getValue() >= right.getValue();
        }

        @Fallback
        @TruffleBoundary
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot apply >= on %s and %s", left.toString(), right.toString()),
                    node);
        }
    }
}
