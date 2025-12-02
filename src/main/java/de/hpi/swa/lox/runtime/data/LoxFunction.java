package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * Representation of Functions that hold their own CallTarget separate from the
 * root program.
 */
@ExportLibrary(InteropLibrary.class)
public class LoxFunction implements TruffleObject {
    public final String name;

    private final RootNode node;

    private final MaterializedFrame outerFunctionFrame;
    public final LoxObject self;

    public LoxFunction(String name, RootNode node, MaterializedFrame outerFunctionFrame, LoxObject self) {
        super();
        this.name = name;
        this.outerFunctionFrame = outerFunctionFrame;
        this.node = node;
        this.self = self;
    }

    public LoxFunction(String name, RootNode node, MaterializedFrame outerFunctionFrame) {
        this(name, node, outerFunctionFrame, null);
    }

    public LoxFunction(LoxObject object, LoxFunction m) {
        this(m.name, m.node, m.outerFunctionFrame, object);
    }

    public RootCallTarget getCallTarget() {
        return node.getCallTarget();
    }

    static LoxFunction getCurrentFunctionFromFrame(Frame frame) {
        // Make use of our off-by-one handling that we use the function object as first
        // argument.
        return (LoxFunction) frame.getArguments()[0];
    }

    static public MaterializedFrame getFrameAtDepthN(VirtualFrame frame, int depth) {
        assert depth > 0;
        LoxFunction func = getCurrentFunctionFromFrame(frame);
        for (int i = depth - 1; i > 0; i--) {
            func = getCurrentFunctionFromFrame(func.outerFunctionFrame);
        }
        return func.outerFunctionFrame;
    }

    /**
     * Create the function arguments used internally.
     * We implicitly define the function object itself as the first argument (so the
     * index is off-by-one),
     * and therefore all user arguments are shifted by one index.
     *
     * Manually specialized for 0-6 arguments to avoid System.arraycopy overhead
     * and enable escape analysis.
     */
    public Object[] createArguments(Object[] userArguments) {
        // Fast path for common cases (0-6 arguments)
        switch (userArguments.length) {
            case 0:
                return new Object[] { this };
            case 1:
                return new Object[] { this, userArguments[0] };
            case 2:
                return new Object[] { this, userArguments[0], userArguments[1] };
            case 3:
                return new Object[] { this, userArguments[0], userArguments[1], userArguments[2] };
            case 4:
                return new Object[] { this, userArguments[0], userArguments[1], userArguments[2], userArguments[3] };
            case 5:
                return new Object[] { this, userArguments[0], userArguments[1], userArguments[2], userArguments[3],
                        userArguments[4] };
            case 6:
                return new Object[] { this, userArguments[0], userArguments[1], userArguments[2], userArguments[3],
                        userArguments[4], userArguments[5] };
            default:
                // Slow path for 7+ arguments (rare case)
                return createArgumentsSlow(userArguments);
        }
    }

    @TruffleBoundary
    private Object[] createArgumentsSlow(Object[] userArguments) {
        Object[] result = new Object[userArguments.length + 1];
        System.arraycopy(userArguments, 0, result, 1, userArguments.length);
        result[0] = this;
        return result;
    }

    /**
     * Retrieves the user argument at the given index in the associated frame.<br/>
     * Note that the index is not the actual index, as we implicitly define the
     * function object
     * itself as the first argument (so the index is off-by-one).
     * 
     * @see #createArguments(Object[])
     */
    public static Object getArgument(VirtualFrame frame, int index) {
        return frame.getArguments()[index + 1];
    }

    @TruffleBoundary
    public String toString() {
        return this.self == null ? "Function " + this.name : this.self.klazz.name + "#" + this.name;
    }

    public static LoxObject getSelf(VirtualFrame frame) {
        return getCurrentFunctionFromFrame(frame).self;
    }

    // ---- Polyglot ----

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(Object[] arguments, @Cached IndirectCallNode callNode) {
        Object[] args = createArguments(arguments);
        return callNode.call(this.getCallTarget(), args);
    }
}
