package de.hpi.swa.lox.runtime.data;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class LoxArray implements TruffleObject {
    private Object[] innerArray;
    int size = 0;

    private LoxArrayIterator iterator;
    private boolean iteratorNeedsReset = true;

    public LoxArray() {
        innerArray = new Object[8];
    }

    public LoxArray(Object[] initialValues) {
        innerArray = initialValues;
        size = initialValues.length;
    }

    public int getSize() {
        return size;
    }

    /**
     * Retrieves the space of the inner array without growing.
     */
    public int getCapacity() {
        return innerArray.length;
    }

    /**
     * Lightweight iterator for array elements.
     * Lazily creates/resets iterator when needed (after array modification or completion).
     */
    public LoxArrayIterator getLoxIterator() {
        if (iterator == null) {
            iterator = new LoxArrayIterator(this);
            iteratorNeedsReset = false;
        } else if (iteratorNeedsReset) {
            iterator.reset();
            iteratorNeedsReset = false;
        }
        return iterator;
    }

    /**
     * Simple iterator implementation that avoids stream/filter/list allocations.
     * Direct array access with manual index tracking. Auto-invalidates parent when exhausted.
     */
    public static final class LoxArrayIterator {
        private final LoxArray array;
        private int index = 0;

        public LoxArrayIterator(LoxArray array) {
            this.array = array;
        }

        public void reset() {
            index = 0;
        }

        public boolean hasNext() {
            boolean result = index < array.size;
            // When iteration completes, mark iterator for reset on next getLoxIterator()
            if (!result && index > 0) {
                array.iteratorNeedsReset = true;
            }
            return result;
        }

        public Object next() {
            return array.get(index++);
        }

        public int nextIndex() {
            return index;
        }
    }

    public Object get(int index) {
        if (innerArray.length <= index || index < 0) {
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
        // Mark iterator for reset since array was modified
        iteratorNeedsReset = true;
    }

    // does not need to grow, but size changes
    public void setInCapacity(int index, Object value) {
        if (index >= size) {
            size = index + 1;
        }
        innerArray[index] = value;
        // Mark iterator for reset since array was modified
        iteratorNeedsReset = true;
    }

    public void setInSize(int index, Object value) {
        innerArray[index] = value;
        // Mark iterator for reset since array was modified
        iteratorNeedsReset = true;
    }

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
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < size; i++) {
            Object element = innerArray[i];
            if (element != null) {
                if (!first) {
                    sb.append(", ");
                }
                // Format doubles without .0 for whole numbers
                if (element instanceof Double) {
                    double d = (double) element;
                    if (d == (long) d) {
                        sb.append(String.format("%d", (long) d));
                    } else {
                        sb.append(d);
                    }
                } else {
                    sb.append(element);
                }
                first = false;
            }
        }
        return open + sb.toString() + close;
    }

    // -----
    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public final boolean isArrayElementInsertable(long index) {
        return true;
    }

    @ExportMessage
    public final boolean isArrayElementModifiable(long index) {
        return true;
    }

    @ExportMessage
    public final boolean isArrayElementReadable(long index) {
        return index >= 0 && index < size;
    }

    @ExportMessage
    public final boolean isArrayElementRemovable(long index) {
        return false;
    }

    @ExportMessage
    public final Object readArrayElement(long index) {
        return get((int) index);
    }

    @ExportMessage
    public final void writeArrayElement(long index, Object value) {
        set((int) index, value);
    }

    @ExportMessage
    public final long getArraySize() {
        return size;
    }

    @ExportMessage
    public final void removeArrayElement(long index) {
        throw new UnsupportedOperationException();
    }
}
