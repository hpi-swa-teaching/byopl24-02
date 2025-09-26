package de.hpi.swa.lox.runtime.data;

import java.util.Arrays;
import java.util.ListIterator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class LoxArray implements TruffleObject {
    private Object[] innerArray;
    int size = 0;

    private boolean iteratorNeedsUpdate = false;
    private boolean trackIterator = true; // Can be disabled for performance

    private ListIterator<Object> iterator;

    public LoxArray() {
        innerArray = new Object[8];
        iterator = buildListIterator();
    }

    public LoxArray(Object[] initialValues) {
        innerArray = initialValues;
        size = initialValues.length;
        iterator = buildListIterator();
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

    @TruffleBoundary
    private ListIterator<Object> buildListIterator() {
        // More efficient iterator creation - avoid stream overhead
        Object[] nonNullElements = new Object[size];
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (innerArray[i] != null) {
                nonNullElements[count++] = innerArray[i];
            }
        }
        // Trim to actual size
        if (count < size) {
            nonNullElements = Arrays.copyOf(nonNullElements, count);
        }
        return Arrays.asList(nonNullElements).listIterator();
    }

    public ListIterator<Object> getLoxIterator() {
        if (iteratorNeedsUpdate) {
            // Lazy when needed
            iterator = buildListIterator();
            iteratorNeedsUpdate = false;
        }
        return iterator;
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
        // Set flag for iterator update
        iteratorNeedsUpdate = true;
    }

    // does not need to grow, but size changes
    public void setInCapacity(int index, Object value) {
        if (index >= size) {
            size = index + 1;
        }
        innerArray[index] = value;
        // Set flag for iterator update
        iteratorNeedsUpdate = true;
    }

    public void setInSize(int index, Object value) {
        innerArray[index] = value;
        // Set flag for iterator update
        iteratorNeedsUpdate = true;
    }

    // Unchecked access methods for performance optimization
    public Object getUnchecked(int index) {
        return innerArray[index];
    }

    public void setUnchecked(int index, Object value) {
        innerArray[index] = value;
        // Set flag for iterator update only if tracking
        if (trackIterator) {
            iteratorNeedsUpdate = true;
        }
    }

    private void ensureCapacity() {
        ensureCapacity(size);
    }

    private void ensureCapacity(int newCapacity) {
        if (newCapacity > innerArray.length) {
            // Use more conservative growth for large arrays
            int growth = innerArray.length < 1024 ? innerArray.length : innerArray.length / 2;
            int newSize = Math.max(newCapacity, innerArray.length + growth);
            innerArray = Arrays.copyOf(innerArray, newSize);
        }
    }

    // Add capacity hints for known usage patterns
    public static LoxArray createWithHint(Object[] initial, int expectedSize) {
        LoxArray result = new LoxArray(initial);
        result.ensureCapacity(expectedSize);
        return result;
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
