package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;

public abstract class NumberBuiltInNode extends BuiltInNodeWithArgs {
    public NumberBuiltInNode(LoxLanguage lang) {
        super(lang, 1);
    }

    @Specialization
    @TruffleBoundary
    static double parseNumber(TruffleString string) {
        return Double.parseDouble(string.toJavaStringUncached());
    }

    @Fallback
    static double fallback(Object arg) {
        return Double.NaN;
    }
}
