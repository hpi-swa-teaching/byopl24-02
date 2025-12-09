package de.hpi.swa.lox.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.runtime.data.LoxArray;

@GenerateInline(true)
@GenerateUncached
@GenerateCached(false)
public abstract class LoxConvertValueNode extends Node {
    @Specialization
    protected double convert(Long object) {
        return (double) object;
    }

    @Specialization
    protected double convert(Double object) {
        return object;
    }

    @Specialization
    protected Object convert(Object object,
            @CachedLibrary(limit = "1") InteropLibrary interop) {
        try {
            if (object instanceof Character c) {
                return TruffleString.fromCodePointUncached(c,
                        TruffleString.Encoding.UTF_8);
            } else if (interop.fitsInLong(object)) {
                return (double) interop.asLong(object);
            } else if (interop.fitsInDouble(object)) {
                return interop.asDouble(object);
            } else if (interop.isString(object)) {
                if (object instanceof TruffleString) {
                    return object;
                }
                return TruffleString.fromJavaStringUncached(
                        interop.asString(object),
                        TruffleString.Encoding.UTF_8);
            }
        } catch (UnsupportedMessageException e) {
            // pass
        }
        return object;
    }

    public abstract Object execute(Node node, Object object);
}