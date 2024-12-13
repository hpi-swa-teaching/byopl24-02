package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.lox.runtime.data.LoxClass;
import de.hpi.swa.lox.runtime.data.LoxFunction;
import de.hpi.swa.lox.runtime.data.LoxObject;

@GenerateUncached
public abstract class LoxLookupMethodNode extends Node {
    public abstract LoxFunction execute(LoxObject object, LoxClass startingKlazz, String name);

    @Specialization(limit = "1")
    public LoxFunction doDefault(LoxObject object, LoxClass startingKlazz, String name,
            @CachedLibrary("startingKlazz") DynamicObjectLibrary dylib) {
        LoxClass klazz = startingKlazz;
        while (klazz != null) {
            var m = dylib.getOrDefault(klazz, name, null);
            if (m != null) {
                return new LoxFunction(object, (LoxFunction) m); // bind method to object
            }
            klazz = (LoxClass) dylib.getOrDefault(klazz, "super", null);
        }
        return null;
    }
}
