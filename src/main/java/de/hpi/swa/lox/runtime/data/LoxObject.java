package de.hpi.swa.lox.runtime.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.lox.nodes.LoxReadPropertyNode;
import de.hpi.swa.lox.nodes.LoxWritePropertyNode;
import de.hpi.swa.lox.runtime.LoxContext;

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
    public Object getMembers(boolean includeInternal) {
        List<Object> keys = new ArrayList<>();
        keys.addAll(Arrays.asList(DynamicObjectLibrary.getUncached().getKeyArray(this)));
        LoxClass klass = this.klazz;
        while (klass != null) {
            // Get members from super classes iteratively
            keys.addAll(Arrays.asList(DynamicObjectLibrary.getUncached().getKeyArray(klass)));
            klass = (LoxClass) DynamicObjectLibrary.getUncached().getOrDefault(klass, "super", null);
        }
        return LoxContext.get(null).getEnv().asGuestValue(keys);
    }

    @ExportMessage
    public boolean isMemberReadable(String member) {
        return ((ArrayList) LoxContext.get(null).getEnv().asHostObject(getMembers(true))).contains(member);
    }

    @ExportMessage
    public Object readMember(String member, @Cached LoxReadPropertyNode readNode) {
        return readNode.execute(member, this);
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
    public void writeMember(String member, Object value, @Cached LoxWritePropertyNode writeNode) {
        writeNode.execute(member, this, value);
    }

    // TODO aber wir können das doch!!
    /**
     * @ExportMessage
     *                public boolean isMmeberInvocable(String member) {
     *                return false;
     *                }
     */
}
