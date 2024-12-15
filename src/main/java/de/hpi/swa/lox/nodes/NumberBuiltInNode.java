package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.runtime.data.LoxNumber;

public abstract class NumberBuiltInNode extends BuiltInNodeWithArgs {
    public NumberBuiltInNode(LoxLanguage lang) {
        super(lang, 1);
    }

    @Specialization
    @TruffleBoundary
    static Object parseNumber(TruffleString string) {
        return new LoxNumber(string.toJavaStringUncached());

    }

    @Fallback
    static Object fallback(Object arg) {
        return LoxNumber.NaN;
    }
}
