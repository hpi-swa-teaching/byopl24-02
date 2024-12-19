package de.hpi.swa.lox.bytecode;

import java.io.IOException;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.LocalAccessor;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.bytecode.ShortCircuitOperation;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.bytecode.ShortCircuitOperation.Operator;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.nodes.LoxCallFunctionNode;
import de.hpi.swa.lox.nodes.LoxLookupMethodNode;
import de.hpi.swa.lox.nodes.LoxRootNode;
import de.hpi.swa.lox.runtime.LoxContext;
import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.GlobalObject;
import de.hpi.swa.lox.runtime.data.LoxArray;
import de.hpi.swa.lox.runtime.data.LoxClass;
import de.hpi.swa.lox.runtime.data.LoxFunction;
import de.hpi.swa.lox.runtime.data.LoxNumber;
import de.hpi.swa.lox.runtime.data.LoxObject;
import de.hpi.swa.lox.runtime.data.Nil;

@GenerateBytecode(//
        languageClass = LoxLanguage.class, enableMaterializedLocalAccesses = true, //
        boxingEliminationTypes = { long.class }, // BUG? boolean.class
        enableUncachedInterpreter = true, //
        enableSerialization = true)
@ShortCircuitOperation(name = "LoxAnd", booleanConverter = LoxBytecodeRootNode.LoxIsTruthy.class, operator = Operator.AND_RETURN_CONVERTED)
@ShortCircuitOperation(name = "LoxOr", booleanConverter = LoxBytecodeRootNode.LoxIsTruthy.class, operator = Operator.OR_RETURN_CONVERTED)
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
        static TruffleString doStrings(TruffleString left, TruffleString right) {
            return left.concatUncached(right, TruffleString.Encoding.UTF_8, false);
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
        public static boolean fromLoxNumber(LoxNumber x) {
            return x.getValue() != 0;
        }

        @Specialization
        public static boolean fromBool(boolean x) {
            return x;
        }

        @Fallback
        public static boolean fromObject(Object x) {
            return x != Nil.INSTANCE;
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
        static LoxArray createEmpty() {
            return new LoxArray();
        }
    }

    @Operation
    public static final class LoxAppendArray {
        @Specialization
        static LoxArray append(LoxArray array, Object value) {
            // Set value on next index of the dynamic array (which is internally equal to
            // the size).
            array.set(array.getSize(), value);
            // Return the array again for stacking of operation.
            return array;
        }
    }

    @Operation
    public static final class LoxWriteArray {
        @Specialization(guards = { "index.getValue().intValue() >= 0",
                "array.getSize() > index.getValue().intValue()" })
        static Void writeArrayInSize(LoxArray array, LoxNumber index, Object value) {
            array.setInSize(index.getValue().intValue(), value);
            return null;
        }

        @Specialization(guards = { "index.getValue().intValue() >= 0",
                "array.getCapacity() > index.getValue().intValue()" }, replaces = "writeArrayInSize")
        static Void writeArrayInCapacity(LoxArray array, LoxNumber index, Object value) {
            array.setInCapacity(index.getValue().intValue(), value);
            return null;
        }

        // Lox number wraps a double, so we need to cast it to int
        @Specialization(guards = "index.getValue().intValue() >= 0", replaces = "writeArrayInCapacity")
        static Void writeArray(LoxArray array, LoxNumber index, Object value) {
            array.set(index.getValue().intValue(), value);
            return null;
        }

        @Fallback
        static Object fallback(Object array, Object index, Object value, @Bind Node node) {
            throw new LoxRuntimeError("array👉index👈 not writable", node);
        }
    }

    @Operation
    public static final class LoxIsArray {
        @Specialization
        static boolean isArray(LoxArray loxArray) {
            return true;
        }

        @Fallback
        static boolean fallback(Object object, @Bind Node node) {
            throw createRuntimeError(object, node);
        }

        @TruffleBoundary
        private static LoxRuntimeError createRuntimeError(Object object, Node node) {
            return new LoxRuntimeError(object.toString() + " is not an LoxArray", node);
        }
    }

    @Operation
    public static final class LoxArrayHasNext {
        @Specialization
        static boolean hasNext(LoxArray loxArray) {
            return loxArray.getIterator().hasNext();
        }

        @Fallback
        static boolean fallback(Object object, @Bind Node node) {
            throw createRuntimeError(object, node);
        }

        @TruffleBoundary
        private static LoxRuntimeError createRuntimeError(Object object, Node node) {
            return new LoxRuntimeError(object.toString() + " is not an LoxArray", node);
        }
    }

    @Operation
    public static final class LoxArrayGetNext {
        @Specialization
        static Object getNext(LoxArray loxArray) {
            return loxArray.getIterator().next();
        }

        @Fallback
        static Object fallback(Object object, @Bind Node node) {
            throw createRuntimeError(object, node);
        }

        @TruffleBoundary
        private static LoxRuntimeError createRuntimeError(Object object, Node node) {
            return new LoxRuntimeError(object.toString() + " is not an LoxArray", node);
        }
    }

    @Operation
    public static final class LoxArrayGetNextIndex {
        @Specialization
        static LoxNumber getNextIndex(LoxArray loxArray) {
            return new LoxNumber(loxArray.getIterator().nextIndex());
        }

        @Fallback
        static LoxNumber fallback(Object object, @Bind Node node) {
            throw createRuntimeError(object, node);
        }

        @TruffleBoundary
        private static LoxRuntimeError createRuntimeError(Object object, Node node) {
            return new LoxRuntimeError(object.toString() + " is not an LoxArray", node);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    @ConstantOperand(type = RootCallTarget.class)
    @ConstantOperand(type = int.class)
    public static final class LoxCreateFunction {

        @Specialization
        static LoxFunction doDefault(VirtualFrame frame, String funName, RootCallTarget callTarget,
                int maxFunctionDepth) {
            MaterializedFrame materializedFunctionFrame = maxFunctionDepth > 0 ? frame.materialize() : null;
            return new LoxFunction(funName, callTarget, materializedFunctionFrame);
        }
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class LoxLoadFunctionArgument {

        // The guard is different from the slides.
        // index <= frame.getArguments().length does not work because of the off-by-one
        // stuff.
        @Specialization(guards = "index < frame.getArguments().length")
        static Object doDefault(VirtualFrame frame, int index) {
            return LoxFunction.getArgument(frame, index);
        }

        @Fallback
        static Object doLoadOutOfBounds(int index) {
            // Use our nil if argument could not be retrieved.
            return Nil.INSTANCE;
        }
    }

    @Operation
    public static final class LoxCallFunction {

        @TruffleBoundary
        @Specialization
        static Object callFunction(LoxFunction function, @Variadic Object[] userArguments,
                @Cached LoxCallFunctionNode callNode) {
            return callNode.execute(function, userArguments);
        }

        @Specialization(limit = "1")
        static Object classInstantiation(LoxClass klazz, @Variadic Object[] userArguments,
                @Cached LoxCallFunctionNode callNode,
                @Cached LoxLookupMethodNode lookupMethodNode) {
            var object = new LoxObject(klazz);

            LoxFunction init = lookupMethodNode.execute(object, klazz, "init");
            if (init != null) {
                callNode.execute(init, userArguments);
            }
            return object;
        }

        @TruffleBoundary
        @Specialization
        static Object doDefault(Object obj, @Variadic Object[] arguments, @Bind Node node) {
            throw new LoxRuntimeError("Cannot call " + obj, node);
        }
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class LoxLoadMaterializedFrameN {

        @Specialization
        public static MaterializedFrame doDefault(VirtualFrame frame, int depth) {
            return LoxFunction.getFrameAtDepthN(frame, depth);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxDeclareClass {
        @Specialization
        @TruffleBoundary
        public static LoxClass declareWithoutSuperclass(String name, Nil nil, @Variadic Object[] methods,
                @CachedLibrary(limit = "1") DynamicObjectLibrary dylib) {
            var klazz = new LoxClass(name);
            for (var m : methods) {
                var method = (LoxFunction) m;
                dylib.putConstant(klazz, method.name, method, 0);
            }
            return klazz;
        }

        @Specialization
        public static LoxClass declare(String name, LoxClass superclass, @Variadic Object[] methods,
                @CachedLibrary(limit = "1") DynamicObjectLibrary dylib) {
            var klazz = declareWithoutSuperclass(name, Nil.INSTANCE, methods, dylib);
            dylib.putConstant(klazz, "super", superclass, 0);
            return klazz;
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxWriteProperty {
        @Specialization(limit = "1")
        public static Object write(String name, LoxObject object, Object value,
                @CachedLibrary("object") DynamicObjectLibrary dylib) {
            dylib.put(object, name, value);
            return value;
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxReadProperty {

        @Specialization
        public static Object read(String name, LoxArray array) {
            if (name.equals("length")) {
                return new LoxNumber(array.getSize());
            } else {
                return Nil.INSTANCE;
            }
        }

        @Specialization(limit = "1")
        public static Object read(String name, LoxObject object,
                @CachedLibrary("object") DynamicObjectLibrary dylib,
                @Cached LoxLookupMethodNode lookupMethodNode) {
            var result = dylib.getOrDefault(object, name, Nil.INSTANCE);
            if (result == Nil.INSTANCE) {
                var method = lookupMethodNode.execute(object, (LoxClass) dylib.getOrDefault(object, "Class", null),
                        name);
                if (method != null) {
                    return method;
                }
            }
            return result;
        }
    }

    @Operation
    public static final class LoxLoadSelf {
        @Specialization
        public static LoxObject loadSelf(VirtualFrame frame) {
            return LoxFunction.getSelf(frame);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxReadSuper {
        @Specialization
        public static Object read(String name, LoxObject object, LoxClass superKlazz,
                @Cached LoxLookupMethodNode lookupMethod) {
            var method = lookupMethod.execute(object, superKlazz, name);
            if (method != null) {
                return method;
            } else {
                return methodNotFound(name);
            }
        }

        @TruffleBoundary
        public static Object methodNotFound(String name) {
            throw new LoxRuntimeError("Method " + name + " not found in super classes", null);
        }
    }

}