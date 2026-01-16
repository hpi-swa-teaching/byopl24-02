package de.hpi.swa.lox.runtime.data;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Number representation (mainly based on Double).
 */
@ExportLibrary(InteropLibrary.class)
public class LoxNumber implements TruffleObject {

    private double internalValue;

    public static final LoxNumber NaN = new LoxNumber(Double.NaN);

    public LoxNumber(String numberText) {
        super();
        try {
            // Try to parse number text to double value.
            // This gives us floating number handling and can also deal with integers.
            // Lox-reference: "Lox has only one kind of number: double-precision floating
            // point".
            this.internalValue = Double.parseDouble(numberText);
        } catch (NumberFormatException e) {
            // If the content is not parseable into a java Double value,
            // our grammar should have resulted in a syntax error before.
            this.internalValue = LoxNumber.NaN.internalValue;
        }
    }

    public LoxNumber(double value) {
        super();
        this.internalValue = value;
    }

    public LoxNumber(int value) {
        super();
        this.internalValue = (double) value;
    }

    public LoxNumber(long value) {
        super();
        this.internalValue = (double) value;
    }

    public double getValue() {
        return internalValue;
    }

    /**
     * We do some special stuff when printing our number representation.
     */
    @Override
    @TruffleBoundary
    public String toString() {
        long longVal = (long) internalValue;
        if (longVal == internalValue) {
            // If we deal with non-floating numbers (integers/longs),
            // we do not need to print the decimal point.
            return Objects.toString(longVal);
        } else {
            // Floating number detected, just use regular Double#toString()
            return Double.toString(internalValue);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != this.getClass()) {
            return false;
        }
        final LoxNumber otherLoxNumber = (LoxNumber) other;
        return this.internalValue == otherLoxNumber.internalValue;
    }

    @ExportMessage
    public boolean isNumber() {
        return true;
    }

    @ExportMessage
    public boolean fitsInInt() {
        long longVal = (long) internalValue;
        return longVal == internalValue && longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE;
    }

    @ExportMessage
    public int asInt() {
        return (int) internalValue;
    }

    @ExportMessage
    public boolean fitsInLong() {
        long longVal = (long) internalValue;
        return longVal == internalValue;
    }

    @ExportMessage
    public long asLong() {
        return (long) internalValue;
    }

    @ExportMessage
    public boolean fitsInFloat() {
        return true;
    }

    @ExportMessage
    public float asFloat() {
        return (float) internalValue;
    }

    @ExportMessage
    public boolean fitsInDouble() {
        return true;
    }

    @ExportMessage
    public double asDouble() {
        return internalValue;
    }

    @ExportMessage
    final boolean fitsInByte() {
        byte byteVal = (byte) internalValue;
        return internalValue >= Byte.MIN_VALUE && internalValue <= Byte.MAX_VALUE
                && internalValue == byteVal;
    }

    @ExportMessage
    final boolean fitsInShort() {
        short shortVal = (short) internalValue;
        return internalValue >= Short.MIN_VALUE && internalValue <= Short.MAX_VALUE
                && internalValue == shortVal;
    }

    @ExportMessage
    final byte asByte() {
        return (byte) internalValue;
    }

    @ExportMessage
    final short asShort() {
        return (short) internalValue;
    }

    @ExportMessage
    final boolean fitsInBigInteger() {
        return false;
    }

}
