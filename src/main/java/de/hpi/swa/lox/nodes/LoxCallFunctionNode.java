package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.lox.runtime.data.LoxFunction;

@GenerateInline(true)
@GenerateUncached
@GenerateCached(false)
public abstract class LoxCallFunctionNode extends Node {
    public abstract Object execute(Node node, Object left, Object right);

    // Specialized for 0 arguments - most common in benchmarks
    @Specialization(limit = "5",
            guards = {"function.getCallTarget() == cachedTarget", "arguments.length == 0"})
    protected static Object doDirect0(LoxFunction function, @Variadic Object[] arguments,
            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
            @Cached("create(cachedTarget)") DirectCallNode directCallNode) {
        Object[] args = new Object[1];
        args[0] = function;
        return directCallNode.call(args);
    }

    // Specialized for 1 argument
    @Specialization(limit = "5",
            guards = {"function.getCallTarget() == cachedTarget", "arguments.length == 1"})
    protected static Object doDirect1(LoxFunction function, @Variadic Object[] arguments,
            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
            @Cached("create(cachedTarget)") DirectCallNode directCallNode) {
        Object[] args = new Object[2];
        args[0] = function;
        args[1] = arguments[0];
        return directCallNode.call(args);
    }

    // Specialized for 2 arguments
    @Specialization(limit = "5",
            guards = {"function.getCallTarget() == cachedTarget", "arguments.length == 2"})
    protected static Object doDirect2(LoxFunction function, @Variadic Object[] arguments,
            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
            @Cached("create(cachedTarget)") DirectCallNode directCallNode) {
        Object[] args = new Object[3];
        args[0] = function;
        args[1] = arguments[0];
        args[2] = arguments[1];
        return directCallNode.call(args);
    }

    // Specialized for 3 arguments
    @Specialization(limit = "5",
            guards = {"function.getCallTarget() == cachedTarget", "arguments.length == 3"})
    protected static Object doDirect3(LoxFunction function, @Variadic Object[] arguments,
            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
            @Cached("create(cachedTarget)") DirectCallNode directCallNode) {
        Object[] args = new Object[4];
        args[0] = function;
        args[1] = arguments[0];
        args[2] = arguments[1];
        args[3] = arguments[2];
        return directCallNode.call(args);
    }

    // Generic fallback for variable arity (no TruffleBoundary, uses loop)
    @Specialization(limit = "5",
            guards = "function.getCallTarget() == cachedTarget", replaces = {"doDirect0", "doDirect1", "doDirect2", "doDirect3"})
    protected static Object doDirectGeneric(LoxFunction function, @Variadic Object[] arguments,
            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
            @Cached("create(cachedTarget)") DirectCallNode directCallNode) {
        Object[] args = new Object[arguments.length + 1];
        args[0] = function;
        for (int i = 0; i < arguments.length; i++) {
            args[i + 1] = arguments[i];
        }
        return directCallNode.call(args);
    }

    @Specialization(replaces = "doDirectGeneric")
    static Object doIndirect(LoxFunction function, @Variadic Object[] arguments,
            @Cached IndirectCallNode callNode) {
        Object[] args = new Object[arguments.length + 1];
        args[0] = function;
        for (int i = 0; i < arguments.length; i++) {
            args[i + 1] = arguments[i];
        }
        return callNode.call(function.getCallTarget(), args);
    }
}