package de.hpi.swa.lox.runtime.data;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class LoxArray {
    private Object[] innerArray;
    int size = 0;

    public LoxArray() {
        innerArray = new Object[8];
    }

    public Object get(int index) {
        if (innerArray.length <= index || index < 0) {
            // TODO warning
            return Nil.INSTANCE;
        }
        var result = innerArray[index];
        if (result != null) {
            return result;
        } else {
            return Nil.INSTANCE;
        }
    }

    public void set(int index, Object value) {
        if (index >= size) {
            size = index + 1;
        }
        if (index >= innerArray.length) {
            this.ensureCapacity();
        }
        innerArray[index] = value;
    }

    @TruffleBoundary
    private void ensureCapacity() {
        innerArray = Arrays.copyOf(innerArray, Math.max(innerArray.length, size) * 2);
    }

    @TruffleBoundary
    public String toString() {
        if (size == 0) {
            return "👉👈";
        }
        String open = "👉";
        String close = "👈";
        String array = Arrays.toString(Arrays.stream(innerArray).filter(a -> a != null).toArray());
        return open + array.substring(1, array.length() - 1) + close;
    }

}
