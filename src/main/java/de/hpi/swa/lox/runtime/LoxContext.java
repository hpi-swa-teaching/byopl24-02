package de.hpi.swa.lox.runtime;

import java.io.OutputStream;
import java.util.Map;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.nodes.BuiltInNode;
import de.hpi.swa.lox.runtime.data.LoxArray;
import de.hpi.swa.lox.runtime.data.LoxFunction;

@Bind.DefaultExpression("get($node)")
public final class LoxContext {

    private final Env env;
    private final DynamicObject globalScope;
    private final Shape globalShape;

    /**
     * Simple DynamicObject subclass for global scope storage.
     * Uses DynamicObject's optimized property storage instead of HashMap.
     */
    private static final class GlobalScope extends DynamicObject {
        GlobalScope(Shape shape) {
            super(shape);
        }
    }

    public LoxContext(LoxLanguage language, TruffleLanguage.Env env, Map<String, BuiltInNode> builtins) {
        super();
        this.env = env;

        // Create global scope as DynamicObject for optimized access
        this.globalShape = Shape.newBuilder()
            .allowImplicitCastIntToLong(true)
            .build();
        this.globalScope = new GlobalScope(globalShape);

        // Use uncached library for initialization only
        DynamicObjectLibrary dylib = DynamicObjectLibrary.getUncached();

        // Load arguments into ARGV
        var ARGV = new LoxArray();
        dylib.put(globalScope, "ARGV", ARGV);
        var args = env.getApplicationArguments();
        for (int i = 0; i < args.length; i++) {
            ARGV.set(i, TruffleString.fromJavaStringUncached(args[i], TruffleString.Encoding.UTF_8));
        }

        // Load builtins
        for (var e : builtins.entrySet()) {
            dylib.put(globalScope, e.getKey(), new LoxFunction(e.getKey(), e.getValue(), null));
        }
    }

    private static final ContextReference<LoxContext> REFERENCE = ContextReference.create(LoxLanguage.class);

    public static LoxContext get(Node node) {
        return REFERENCE.get(node);
    }

    public Env getEnv() {
        return env;
    }

    public OutputStream getOutput() {
        return env.out();
    }

    public DynamicObject getGlobalScope() {
        return globalScope;
    }
}
