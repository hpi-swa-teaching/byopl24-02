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
import de.hpi.swa.lox.runtime.data.LoxNumber;

@GenerateUncached
public abstract class LoxConvertValueNode extends Node {
    @Specialization
    protected Object convert(Long object) {
        return new LoxNumber(object);
    }

    @Specialization
    protected Object convert(Double object) {
        return new LoxNumber(object);
    }

    @Specialization
    protected Object convert(LoxNumber object) {
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
                return new LoxNumber(interop.asLong(object));
            } else if (interop.fitsInDouble(object)) {
                return new LoxNumber(interop.asDouble(object));
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