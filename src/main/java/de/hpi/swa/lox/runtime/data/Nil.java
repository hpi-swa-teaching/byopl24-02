package de.hpi.swa.lox.runtime.data;

public final class Nil {
    
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
