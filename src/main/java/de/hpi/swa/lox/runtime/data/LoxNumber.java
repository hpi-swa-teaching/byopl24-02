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

    private Double internalValue;

    public static final LoxNumber NaN = new LoxNumber(Double.NaN);

    public LoxNumber(String numberText) {
        try {
            // Try to parse number text to Double value.
            // This gives us floating number handling and can also deal with integers.
            // Lox-reference: "Lox has only one kind of number: double-precision floating
            // point".
            this.internalValue = Double.valueOf(numberText);
        } catch (NumberFormatException e) {
            // If the content is not parseable into a java Double value,
            // our grammar should have resulted in a syntax error before.
            this.internalValue = LoxNumber.NaN.internalValue;
        }
    }

    public LoxNumber(Double value) {
        this.internalValue = value;
    }

    public LoxNumber(int value) {
        this.internalValue = (double) value;
    }

    public LoxNumber(long value) {
        this.internalValue = (double) value;
    }

    public Double getValue() {
        return internalValue;
    }

    /**
     * We do some special stuff when printing our number representation.
     */
    @Override
    @TruffleBoundary
    public String toString() {
        if (internalValue.longValue() == internalValue.doubleValue()) {
            // If we deal with non-floating numbers (integers/longs),
            // we do not need to print the decimal point.
            return Objects.toString(internalValue.longValue());
        } else {
            // Floating number detected, just use regular Double#toString()
            return internalValue.toString();
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
        return this.getValue().equals(otherLoxNumber.getValue());
    }

    @ExportMessage
    public boolean isNumber() {
        return true;
    }

    @ExportMessage
    public boolean fitsInInt() {
        return internalValue.longValue() == internalValue.doubleValue();
    }

    @ExportMessage
    public int asInt() {
        return internalValue.intValue();
    }

    @ExportMessage
    public boolean fitsInLong() {
        return internalValue.longValue() == internalValue.doubleValue();
    }

    @ExportMessage
    public long asLong() {
        return internalValue.longValue();
    }

    @ExportMessage
    public boolean fitsInFloat() {
        return true;
    }

    @ExportMessage
    public float asFloat() {
        return internalValue.floatValue();
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
        return internalValue >= Byte.MIN_VALUE && internalValue <= Byte.MAX_VALUE
                && internalValue == internalValue.byteValue();
    }

    @ExportMessage
    final boolean fitsInShort() {
        return internalValue >= Short.MIN_VALUE && internalValue <= Short.MAX_VALUE
                && internalValue == internalValue.shortValue();
    }

    @ExportMessage
    final byte asByte() {
        return internalValue.byteValue();
    }

    @ExportMessage
    final short asShort() {
        return internalValue.shortValue();
    }

    @ExportMessage
    final boolean fitsInBigInteger() {
        return false;
    }

}
