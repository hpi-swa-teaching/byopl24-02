package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;

public abstract class StringBuiltInNode extends BuiltInNodeWithArgs {
    public StringBuiltInNode(LoxLanguage lang) {
        super(lang, 1);
    }

    @Specialization
    @TruffleBoundary
    static TruffleString parseDouble(double value) {
        // Format double: omit ".0" for integer values
        String formatted;
        if (value == (long) value) {
            formatted = Long.toString((long) value);
        } else {
            formatted = Double.toString(value);
        }
        return TruffleString.fromJavaStringUncached(formatted, TruffleString.Encoding.UTF_8);
    }

    @Specialization
    @TruffleBoundary
    static TruffleString parseString(Object string) {
        return TruffleString.fromJavaStringUncached(string.toString(), TruffleString.Encoding.UTF_8);

    }
}
