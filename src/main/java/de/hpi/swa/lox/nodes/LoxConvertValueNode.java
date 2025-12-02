package de.hpi.swa.lox.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.runtime.data.LoxArray;

@GenerateUncached
public abstract class LoxConvertValueNode extends Node {
    @Specialization
    protected double convertLong(Long object) {
        return object.doubleValue();
    }

    @Specialization
    protected double convertDouble(Double object) {
        return object;  // Return primitive double
    }

    @Specialization
    protected Object convert(Object object,
            @CachedLibrary(limit = "1") InteropLibrary interop) {
        try {
            if (object instanceof Character c) {
                return TruffleString.fromCodePointUncached(c,
                        TruffleString.Encoding.UTF_8);
            } else if (interop.fitsInDouble(object)) {
                return interop.asDouble(object);  // Return primitive double
            } else if (interop.fitsInLong(object)) {
                return (double) interop.asLong(object);  // Convert to double
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

    public abstract Object execute(Object object);
}