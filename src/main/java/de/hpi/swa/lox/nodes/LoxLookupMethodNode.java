package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.lox.runtime.data.LoxClass;
import de.hpi.swa.lox.runtime.data.LoxFunction;
import de.hpi.swa.lox.runtime.data.LoxObject;

@GenerateInline(true)
@GenerateUncached
@GenerateCached(false)
public abstract class LoxLookupMethodNode extends Node {
    public abstract LoxFunction execute(Node node, LoxObject object, LoxClass startingKlazz, String name);

    @Specialization(limit = "1", guards = { "startingClass == cachedStartingClass", "name == cachedName" })
    public LoxFunction doCached(LoxObject obj, LoxClass startingClass, String name,
            @Cached("name") String cachedName, @Cached("startingClass") LoxClass cachedStartingClass,
            @CachedLibrary("startingClass") DynamicObjectLibrary dylib,
            @Cached("lookupMethod(startingClass, name, dylib)") LoxFunction cachedMethod) {
        if (cachedMethod != null) {
            return new LoxFunction(obj, cachedMethod); // bind method to object
        } else {
            return null;
        }
    }

    @Specialization(limit = "1", replaces = "doCached")
    public LoxFunction doUncached(LoxObject obj, LoxClass startingClass, String name,
            @CachedLibrary("startingClass") DynamicObjectLibrary dylib) {
        var method = lookupMethod(startingClass, name, dylib);
        if (method != null) {
            return new LoxFunction(obj, method); // bind method to object
        } else {
            return null;
        }
    }

    public LoxFunction lookupMethod(LoxClass startingClass, String name,
            @CachedLibrary("startingClass") DynamicObjectLibrary dylib) {
        LoxClass klazz = startingClass;
        while (klazz != null) {
            var m = dylib.getOrDefault(klazz, name, null);
            if (m != null) {
                return (LoxFunction) m;
            }
            klazz = (LoxClass) dylib.getOrDefault(klazz, "super", null);
        }
        return null;
    }

}
