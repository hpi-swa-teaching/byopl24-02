package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.LoxObject;

@GenerateUncached
public abstract class LoxWritePropertyNode extends Node {
    public abstract Object execute(String name, Object object, Object value);

    @Specialization(limit = "1")
    public static Object write(String name, LoxObject object, Object value,
            @CachedLibrary("object") DynamicObjectLibrary dylib) {
        dylib.put(object, name, value);
        return value;
    }

    @Specialization(limit = "1")
    public static Object interopWrite(String name, Object obj, Object value,
            @CachedLibrary(limit = "1") InteropLibrary interop,
            @Bind Node node) {
        try {
            interop.writeMember(obj, name, value);
        } catch (UnsupportedTypeException | UnsupportedMessageException | UnknownIdentifierException e) {
            error(name, obj, node);
        }
        return value;
    }

    @TruffleBoundary
    static Object error(String name, Object obj, @Bind Node node) {
        throw new LoxRuntimeError("Cannot write property " + name + " of " + obj, node);

    }

}
