package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.LoxArray;
import de.hpi.swa.lox.runtime.data.LoxClass;
import de.hpi.swa.lox.runtime.data.LoxObject;
import de.hpi.swa.lox.runtime.data.Nil;

@GenerateUncached
public abstract class LoxReadPropertyNode extends Node {
    public abstract Object execute(String name, Object object);

    @Specialization
    public static Object read(String name, LoxArray array) {
        if (name.equals("length")) {
            return (double) array.getSize();
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

    @Specialization
    public static Object read(String name, Object obj, @Bind Node node,
            @CachedLibrary(limit = "1") InteropLibrary interop) {
        try {
            return interop.readMember(obj, name);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            return error(name, obj, node);
        }
    }

    @TruffleBoundary
    static Object error(String name, Object obj, @Bind Node node) {
        throw new LoxRuntimeError("Cannot read property " + name + " of " + obj, node);

    }

}
