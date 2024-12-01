package de.hpi.swa.lox.runtime.data;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;

/** 
 * Number representation (mainly based on Double).
 */
public class LoxNumber implements TruffleObject {

    private Double internalValue;

    public LoxNumber(String numberText) {
        try {
            // Try to parse number text to Double value.
            // This gives us floating number handling and can also deal with integers.
            // Lox-reference: "Lox has only one kind of number: double-precision floating point".
            this.internalValue = Double.valueOf(numberText);
        } catch (NumberFormatException e) {
            // If the content is not parseable into a java Double value,
            // our grammar should have resulted in a syntax error before.
        }
    }

    public LoxNumber(Double value) {
        this.internalValue = value;
    }

    public LoxNumber(int value) {
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
        // We need to apply a double comparison, that means we can't really use the == operator!
        // see https://stackoverflow.com/a/6837237.
        final float epsilon = 5.96e-08f;
        return Math.abs(this.getValue() / otherLoxNumber.getValue() - 1) < epsilon;
    }
}
