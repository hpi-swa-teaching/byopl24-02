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
import com.oracle.truffle.api.bytecode.ShortCircuitOperation;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.bytecode.ShortCircuitOperation.Operator;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.nodes.LoxCallFunctionNode;
import de.hpi.swa.lox.nodes.LoxConvertValueNode;
import de.hpi.swa.lox.nodes.LoxLookupMethodNode;
import de.hpi.swa.lox.nodes.LoxReadPropertyNode;
import de.hpi.swa.lox.nodes.LoxRootNode;
import de.hpi.swa.lox.nodes.LoxWritePropertyNode;
import de.hpi.swa.lox.runtime.LoxContext;
import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.GlobalObject;
import de.hpi.swa.lox.runtime.data.LoxArray;
import de.hpi.swa.lox.runtime.data.LoxClass;
import de.hpi.swa.lox.runtime.data.LoxFunction;
import de.hpi.swa.lox.runtime.data.LoxObject;
import de.hpi.swa.lox.runtime.data.Nil;

@GenerateBytecode(languageClass = LoxLanguage.class, enableMaterializedLocalAccesses = true, //
        boxingEliminationTypes = { long.class, double.class }, // Enable boxing elimination for primitives
        enableUncachedInterpreter = true, //
        enableSerialization = true, enableRootTagging = true, enableRootBodyTagging = false, enableTagInstrumentation = true)
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
        static void doDouble(double value, @Bind LoxContext context) {
            var out = context.getOutput();
            try {
                // Format double without .0 for whole numbers
                String formatted;
                if (value == (long) value) {
                    formatted = String.format("%d", (long) value);
                } else {
                    formatted = String.valueOf(value);
                }
                out.write(formatted.getBytes());
                out.write(System.lineSeparator().getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
        static double doDouble(double value) {
            return -value;
        }

        @Fallback
        @TruffleBoundary
        static Object doOtherTypes(Object value, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot negate %s", value), node);
        }
    }

    @Operation
    public static final class LoxAdd {
        @Specialization
        static double doDoubles(double left, double right) {
            return left + right;
        }

        @Specialization
        static TruffleString doStrings(TruffleString left, TruffleString right) {
            return left.concatUncached(right, TruffleString.Encoding.UTF_8, false);
        }

        @Fallback
        @TruffleBoundary
        static Object doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot add %s and %s", left.toString(), right.toString()), node);
        }
    }

    @Operation
    public static final class LoxSubtract {
        @Specialization
        static double doDoubles(double left, double right) {
            return left - right;
        }

        @Fallback
        @TruffleBoundary
        static Object doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot subtract %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxMultiply {
        @Specialization
        static double doDoubles(double left, double right) {
            return left * right;
        }

        @Fallback
        @TruffleBoundary
        static Object doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot multiply %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxDivide {
        @Specialization
        static double doDoubles(double left, double right, @Bind Node node) {
            if (right == 0) {
                throw new LoxRuntimeError("Division by zero", node);
            }
            return left / right;
        }

        @Fallback
        @TruffleBoundary
        static Object doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot divide %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxInequal {
        @Specialization
        static boolean doDoubles(double left, double right) {
            return left != right;
        }

        @Specialization
        static boolean doDefault(Object left, Object right) {
            return left != right;
        }
    }

    @Operation
    public static final class LoxEqual {
        @Specialization
        static boolean doDoubles(double left, double right) {
            return left == right;
        }

        @Specialization
        static boolean doStrings(TruffleString left, TruffleString right,
                @Cached TruffleString.EqualNode equalNode) {
            return equalNode.execute(left, right, TruffleString.Encoding.UTF_8);
        }

        @Fallback
        @TruffleBoundary
        static boolean doDefault(Object left, Object right) {
            return left.equals(right);
        }
    }

    @Operation
    public static final class LoxLess {
        @Specialization
        static boolean doDoubles(double left, double right) {
            return left < right;
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
        static boolean doDoubles(double left, double right) {
            return left <= right;
        }

        @Fallback
        @TruffleBoundary
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot apply <= on %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    public static final class LoxGreater {
        @Specialization
        static boolean doDoubles(double left, double right) {
            return left > right;
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
        static boolean doDoubles(double left, double right) {
            return left >= right;
        }

        @Fallback
        @TruffleBoundary
        static boolean doOtherTypes(Object left, Object right, @Bind Node node) {
            throw new LoxRuntimeError(String.format("Cannot apply >= on %s and %s", left.toString(), right.toString()),
                    node);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxWriteGlobalVariable {
        @Specialization
        static void doDefault(String variableName,
                Object value,
                @Bind LoxContext loxContext,
                @Bind Node node,
                @CachedLibrary(limit = "3") DynamicObjectLibrary dylib) {
            DynamicObject globalScope = loxContext.getGlobalScope();

            // Check if declared (no TruffleBoundary!)
            if (!dylib.containsKey(globalScope, variableName)) {
                throw new LoxRuntimeError("Variable " + variableName + " was not declared", node);
            }

            dylib.put(globalScope, variableName, value);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxReadGlobalVariable {
        @Specialization
        static Object doDefault(
                String variableName,
                @Bind LoxContext loxContext,
                @Bind Node node,
                @CachedLibrary(limit = "3") DynamicObjectLibrary dylib) {
            DynamicObject globalScope = loxContext.getGlobalScope();

            // Check if declared (no TruffleBoundary!)
            if (!dylib.containsKey(globalScope, variableName)) {
                throw createNotDeclaredError(variableName, node);
            }

            Object value = dylib.getOrDefault(globalScope, variableName, null);
            if (value == null) {
                // if not defined --> also RuntimeError
                throw createNotDefinedError(variableName, node);
            }
            return value;
        }

        @TruffleBoundary
        static LoxRuntimeError createNotDeclaredError(String variableName, Node node) {
            return new LoxRuntimeError("Variable " + variableName + " was not declared", node);
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
                @Bind Node node,
                @CachedLibrary(limit = "3") DynamicObjectLibrary dylib) {
            DynamicObject globalScope = loxContext.getGlobalScope();

            // Check if already declared (no TruffleBoundary!)
            if (dylib.containsKey(globalScope, variableName)) {
                printWarning(variableName, loxContext);
            }

            // Declare variable with null value
            dylib.put(globalScope, variableName, null);
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
        if (object instanceof Double)
            return ((Double) object) != 0.0;
        return true;
    }

    @Operation
    public static final class LoxIsTruthy {

        @Specialization
        public static boolean fromDouble(double x) {
            return x != 0.0;
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
        @Specialization
        static Object readArrayDouble(LoxArray array, double index) {
            if (index < 0) {
                throw new LoxRuntimeError("array👉index👈 not readable", null);
            }
            return array.get((int) index);
        }

        @Fallback
        static Object fallback(Object array, Object index, @Bind Node node) {
            throw new LoxRuntimeError("array👉index👈 not readable", node);
        }

    }

    @Operation
    public static final class LoxNewArray {
        @Specialization
        static LoxArray create(@Variadic Object[] initialValues) {
            return new LoxArray(initialValues);
        }
    }

    @Operation
    public static final class LoxWriteArray {
        @Specialization
        static Void writeArrayDouble(LoxArray array, double index, Object value) {
            if (index < 0) {
                throw new LoxRuntimeError("array👉index👈 not writable", null);
            }
            array.set((int) index, value);
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
            return loxArray.getLoxIterator().hasNext();
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
            return loxArray.getLoxIterator().next();
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
        static double getNextIndex(LoxArray loxArray) {
            return (double) loxArray.getLoxIterator().nextIndex();
        }

        @Fallback
        static double fallback(Object object, @Bind Node node) {
            throw createRuntimeError(object, node);
        }

        @TruffleBoundary
        private static LoxRuntimeError createRuntimeError(Object object, Node node) {
            return new LoxRuntimeError(object.toString() + " is not an LoxArray", node);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    @ConstantOperand(type = RootNode.class)
    @ConstantOperand(type = int.class)
    public static final class LoxCreateFunction {

        @Specialization
        static LoxFunction doDefault(VirtualFrame frame, String funName, RootNode node,
                int maxFunctionDepth) {
            MaterializedFrame materializedFunctionFrame = maxFunctionDepth > 0 ? frame.materialize() : null;
            return new LoxFunction(funName, node, materializedFunctionFrame);
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

        @Specialization
        static Object callFunction(LoxFunction function, @Variadic Object[] userArguments,
                @Bind("this") Node node,
                @Cached LoxCallFunctionNode callNode) {
            return callNode.execute(node, function, userArguments);
        }

        @Specialization(limit = "1")
        static Object classInstantiation(LoxClass klazz, @Variadic Object[] userArguments,
                @Bind("this") Node node,
                @Cached LoxCallFunctionNode callNode,
                @Cached LoxLookupMethodNode lookupMethodNode) {
            var object = new LoxObject(klazz);

            LoxFunction init = lookupMethodNode.execute(node, object, klazz, "init");
            if (init != null) {
                callNode.execute(node, init, userArguments);
            }
            return object;
        }

        @Specialization
        static Object doDefault(Object obj, @Variadic Object[] arguments, @Bind Node node,
                @CachedLibrary(limit = "1") InteropLibrary interop) {
            if (interop.isExecutable(obj)) {
                try {
                    return interop.execute(obj, arguments);
                } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                    return error(obj, node);
                }
            } else {
                try {
                    return interop.instantiate(obj, arguments);
                } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                    return error(obj, node);
                }
            }
        }

        @TruffleBoundary
        static Object error(Object obj, Node node) {
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

        @Specialization
        public static Object write(String name, Object object, Object value,
                @Bind("this") Node node,
                @Cached LoxWritePropertyNode writeProperty) {
            return writeProperty.execute(node, name, object, value);
        }
    }

    @Operation
    @ConstantOperand(type = String.class)
    public static final class LoxReadProperty {

        @Specialization
        public static Object read(String name, Object obj,
                @Bind("this") Node node,
                @Cached LoxReadPropertyNode readProperty) {
            return readProperty.execute(node, name, obj);
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
                @Bind("this") Node node,
                @Cached LoxLookupMethodNode lookupMethod) {
            var method = lookupMethod.execute(node, object, superKlazz, name);
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

    @Operation
    public static final class LoxValue {
        @Specialization
        static Object doDefault(Object value,
                @Bind("this") Node node,
                @Cached LoxConvertValueNode convertValueNode) {
            return convertValueNode.execute(node, value);
        }
    }

    @Operation(tags = DebuggerTags.AlwaysHalt.class)
    public static final class LoxHalt {
        @TruffleBoundary
        @Specialization
        static void doDefault(@Bind LoxContext context) {
            System.err.println("Halt");
        }
    }
}