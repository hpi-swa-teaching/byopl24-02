package de.hpi.swa.lox;

import de.hpi.swa.lox.nodes.BuiltInNode;
import de.hpi.swa.lox.nodes.ClockBuiltInNodeGen;
import de.hpi.swa.lox.nodes.LoadBuiltInNodeGen;
import de.hpi.swa.lox.nodes.NumberBuiltInNodeGen;
import de.hpi.swa.lox.nodes.StringBuiltInNodeGen;
import de.hpi.swa.lox.parser.LoxBytecodeCompiler;

import com.oracle.truffle.api.source.Source;

import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;

import de.hpi.swa.lox.runtime.LoxContext;

@TruffleLanguage.Registration(id = LoxLanguage.ID)
public class LoxLanguage extends TruffleLanguage<LoxContext> {

    public static final String ID = "lox";
    private Map<String, BuiltInNode> builtins;

    public LoxLanguage() {
    }

    @Override
    protected LoxContext createContext(Env env) {
        return new LoxContext(this, env, getBuiltins());
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        Source source = request.getSource();
        RootCallTarget rootTarget = LoxBytecodeCompiler.parseLox(this, source);
        return rootTarget;
    }

    private Map<String, BuiltInNode> getBuiltins() {
        if (builtins == null) {
            builtins = Map
                    .of(
                            "clock", ClockBuiltInNodeGen.create(this),
                            "Number", NumberBuiltInNodeGen.create(this),
                            "String", StringBuiltInNodeGen.create(this),
                            "load", LoadBuiltInNodeGen.create(this));

        }
        return builtins;
    }
}
