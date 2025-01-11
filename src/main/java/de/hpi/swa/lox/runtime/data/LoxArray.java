package de.hpi.swa.lox.runtime.data;

import java.util.Arrays;
import java.util.ListIterator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class LoxArray {
    private Object[] innerArray;
    int size = 0;

    private boolean iteratorNeedsUpdate = false;

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
        return Arrays.asList(innerArray)
                     .stream()
                     // We should not iterate through our unassigned indices!
                     .filter(element -> element != null)
                     .toList().listIterator();
    }

    public ListIterator<Object> getIterator() {
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
