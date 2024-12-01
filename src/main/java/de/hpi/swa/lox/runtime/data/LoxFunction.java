package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Representation of Functions that hold their own CallTarget separate from the
 * root program.
 */
public class LoxFunction {

    private final String name;

    private final RootCallTarget callTarget;

    private final MaterializedFrame outerFunctionFrame;

    public LoxFunction(String name, RootCallTarget callTarget, MaterializedFrame outerFunctionFrame) {
        this.name = name;
        this.callTarget = callTarget;
        this.outerFunctionFrame = outerFunctionFrame;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    static LoxFunction getCurrentFunctionFromFrame(Frame frame) { 
        // Make use of our off-by-one handling that we use the function object as first argument.   
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
     * We implicitly define the function object itself as the first argument (so the index is off-by-one),
     * and therefore all user arguments are shifted by one index.
     */
    @TruffleBoundary
    public Object[] createArguments(Object[] userArguments) {
        Object[] result = new Object[userArguments.length + 1];
        System.arraycopy(userArguments, 0, result, 1, userArguments.length);
        result[0] = this; // give the static function itself as first argument
        return result;
    }

    /**
     * Retrieves the user argument at the given index in the associated frame.<br/>
     * Note that the index is not the actual index, as we implicitly define the function object
     * itself as the first argument (so the index is off-by-one).
     * @see #createArguments(Object[])
     */
    public static Object getArgument(VirtualFrame frame, int index) {
        return frame.getArguments()[index + 1];
    }

    @TruffleBoundary
    public String toString() {
        return "Function " + this.name;
    }
}
