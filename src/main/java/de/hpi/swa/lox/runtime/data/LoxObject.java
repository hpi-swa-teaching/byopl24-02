package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.lox.nodes.LoxReadPropertyNode;
import de.hpi.swa.lox.nodes.LoxWritePropertyNode;

@ExportLibrary(InteropLibrary.class)
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

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal,
            @CachedLibrary("this") DynamicObjectLibrary dylib) {
        Object[] keys = dylib.getKeyArray(this);
        return new Keys(keys);
    }

    @ExportMessage
    public boolean isMemberReadable(String member,
            @CachedLibrary("this") DynamicObjectLibrary dylib) {
        return dylib.containsKey(this, member);
    }

    @ExportMessage
    public Object readMember(String member, @Bind("$node") Node node, @Cached LoxReadPropertyNode readNode) {
        return readNode.execute(node, member, this);
    }

    @ExportMessage
    public boolean isMemberModifiable(String member) {
        return true;
    }

    @ExportMessage
    public boolean isMemberInsertable(String member) {
        return true;
    }

    @ExportMessage
    public void writeMember(String member, Object value, @Bind("$node") Node node, @Cached LoxWritePropertyNode writeNode) {
        writeNode.execute(node, member, this, value);
    }

    // TODO aber wir können das doch!!
    /**
     * @ExportMessage
     *                public boolean isMmeberInvocable(String member) {
     *                return false;
     *                }
     */

    /**
     * Lightweight wrapper for object keys that implements InteropLibrary
     * for efficient member enumeration without allocations.
     */
    @ExportLibrary(InteropLibrary.class)
    static final class Keys implements TruffleObject {
        private final Object[] keys;

        Keys(Object[] keys) {
            this.keys = keys;
        }

        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return keys.length;
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index >= 0 && index < keys.length;
        }

        @ExportMessage
        Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (!isArrayElementReadable(index)) {
                throw InvalidArrayIndexException.create(index);
            }
            return keys[(int) index];
        }
    }
}
