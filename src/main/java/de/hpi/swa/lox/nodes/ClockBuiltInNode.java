package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.runtime.data.LoxNumber;

public abstract class ClockBuiltInNode extends BuiltInNodeWithArgs {

    public ClockBuiltInNode(LoxLanguage lang) {
        super(lang, 0);
    }

    @Specialization
    @TruffleBoundary
    static Object getTime(Object string) {
        return new LoxNumber((double) System.nanoTime() / 1_000_000_000.0);

    }
}
