package de.hpi.swa.lox.runtime.data;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Storage of global variables, abstracted cause of TruffleBoundaries.
 * Cleaned up when whole program ends.
 */
public class GlobalObject {
    
    private final Map<String, Object> globals = new HashMap<>();

    @TruffleBoundary
    public Object get(String name) {
        return globals.get(name);
    }

    @TruffleBoundary
    public Object getOrDefault(String name, Object defaultValue) {
        return globals.getOrDefault(name, defaultValue);
    }

    @TruffleBoundary
    public void set(String name, Object value) {
        globals.put(name, value);
    }

    @TruffleBoundary
    public boolean hasKey(String name) {
        return globals.containsKey(name);
    }
}
