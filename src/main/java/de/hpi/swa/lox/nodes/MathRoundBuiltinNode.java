package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.runtime.LoxRuntimeError;
import de.hpi.swa.lox.runtime.data.LoxNumber;

public abstract class MathRoundBuiltinNode extends BuiltInNodeWithArgs {
    public MathRoundBuiltinNode(LoxLanguage lang) {
        super(lang, 1);
    }

    @Specialization
    static LoxNumber roundDouble(LoxNumber number) {
        return new LoxNumber(Math.round(number.getValue()));
    }

    @Fallback
    @TruffleBoundary
    static Object fallback(Object arg, @Bind Node node) {
        throw new LoxRuntimeError("Type Error: Argument must be a Number: " + arg, node);
    }
}