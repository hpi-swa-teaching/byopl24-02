package de.hpi.swa.lox.bytecode;

import java.io.IOException;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.LocalAccessor;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.nodes.LoxRootNode;
import de.hpi.swa.lox.runtime.LoxContext;
import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.GlobalObject;
import de.hpi.swa.lox.runtime.data.LoxArray;
import de.hpi.swa.lox.runtime.data.LoxNumber;
import de.hpi.swa.lox.runtime.data.Nil;

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
        static boolean doOtherTypes(Object value, @Bind Node node) {
            return !isTruthy(value);
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

        @Fallback
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            return isTruthy(left) || isTruthy(right);
        }

    }

    @Operation
    public static final class LoxAnd {
        @Specialization
        static boolean doBoolean(boolean left, boolean right) {
            return left && right;
        }

        @Fallback
        static boolean doOtherTypes(Object left, Object right) {
            return isTruthy(left) && isTruthy(right);
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

    @TruffleBoundary
    static Object checkDeclared(String variableName, GlobalObject globalObject, @Bind Node node) {
        if (!globalObject.hasKey(variableName)) {
            throw new LoxRuntimeError("Variable " + variableName + " was not declared", node);
        }
        return globalObject.get(variableName);
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxWriteGlobalVariable {
        @Specialization
        static void doDefault(String variableName,
                Object value,
                @Bind LoxContext loxContext,
                @Bind Node node) {
            GlobalObject globalObject = loxContext.getGlobalObject();
            checkDeclared(variableName, globalObject, node);
            globalObject.set(variableName, value);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxReadGlobalVariable {
        @Specialization
        static Object doDefault(
                String variableName,
                @Bind LoxContext loxContext,
                @Bind Node node) {
            GlobalObject globalObject = loxContext.getGlobalObject();
            // if not declared --> RuntimeError thrown
            var declaredResult = checkDeclared(variableName, globalObject, node);
            if (declaredResult == null) {
                // if not defined --> also RuntimeError
                throw createNotDefinedError(variableName, node);
            }
            return declaredResult;
        }

        @TruffleBoundary
        static LoxRuntimeError createNotDefinedError(String variableName, Node node) {
            return new LoxRuntimeError("Variable " + variableName + " was not defined", node);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxDeclareGlobalVariable {
        @Specialization
        static void doDefault(String variableName,
                @Bind LoxContext loxContext,
                @Bind Node node) {
            GlobalObject globalObject = loxContext.getGlobalObject();
            if (globalObject.get(variableName) != null) {
                printWarning(variableName, loxContext);
            }
            globalObject.set(variableName, null);
        }

        @TruffleBoundary
        private static void printWarning(String variableName, LoxContext loxContext) {
            var out = loxContext.getOutput();
            try {
                out.write(("Warning: Variable " + variableName +
                        " was already declared and defined, resetting its value to null").getBytes());
                out.write(System.lineSeparator().getBytes());
                out.flush();
            } catch (IOException e) {
                // pass
            }
        }
    }

    @Operation
    @ConstantOperand(type = LocalAccessor.class)
    public static final class LoxCheckLocalDefined {
        @Specialization
        static void doDefault(VirtualFrame frame, LocalAccessor accessor,
                @Bind BytecodeNode bytecodeNode,
                @Bind LoxContext loxContext,
                @Bind Node node) {
            if (accessor.isCleared(bytecodeNode, frame)) {
                throw createNotDefinedError(accessor, node);
            }
            ;
        }

        @TruffleBoundary
        static LoxRuntimeError createNotDefinedError(LocalAccessor variableNameAccessor, Node node) {
            return new LoxRuntimeError("Local variable " + variableNameAccessor.toString() + " was not defined.", node);
        }
    }

    static private boolean isTruthy(Object object) {
        // different to the slides, we decided to treat 0 as false
        if (object == Nil.INSTANCE)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        if (object instanceof LoxNumber)
            return ((LoxNumber) object).getValue() != 0;
        return true;
    }

    @Operation
    public static final class LoxIsTruthy {

        @Specialization
        static boolean doBoolean(boolean value) {
            return value;
        }

        @Fallback
        static boolean doDefault(Object value) {
            return isTruthy(value);
        }
    }

    @Operation
    public static final class LoxReadArray {
        @Specialization(guards = "index.getValue().intValue() >= 0")
        static Object readArray(LoxArray array, LoxNumber index) {
            return array.get(index.getValue().intValue());
        }

        @Fallback
        static Object fallback(Object array, Object index, @Bind Node node) {
            throw new LoxRuntimeError("array👉index👈 not readable", node);
        }

    }

    @Operation
    public static final class LoxNewArray {
        @Specialization
        static Object fallback() {
            return new LoxArray();
        }
    }

    @Operation
    public static final class LoxWriteArray {
        // Lox number wraps a double, so we need to cast it to int
        @Specialization(guards = "index.getValue().intValue() >= 0")
        static Void writeArray(LoxArray array, LoxNumber index, Object value) {
            array.set(index.getValue().intValue(), value);
            return null;
        }

        @Fallback
        static Object fallback(Object array, Object index, Object value, @Bind Node node) {
            throw new LoxRuntimeError("array👉index👈 not writable", node);
        }
    }

}
