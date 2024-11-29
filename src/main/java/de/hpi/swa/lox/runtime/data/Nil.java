package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.interop.TruffleObject;

public final class Nil implements TruffleObject {
    
    /**
     * Singleton instance of our "no value" representation.
     */
    public static final Nil INSTANCE = new Nil();

    private Nil() {}

    @Override
    public String toString() {
        return "nil";
    }

}
