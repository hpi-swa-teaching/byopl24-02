package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.object.DynamicObject;

public class LoxObject extends DynamicObject {
    public final LoxClass klazz;

    public LoxObject(LoxClass klass) {
        super(klass.instanceShape);
        this.klazz = klass;
    }

    @Override
    public String toString() {
        return klazz.name;
    }
}
