package de.hpi.swa.lox.nodes;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.lox.LoxLanguage;
import de.hpi.swa.lox.parser.LoxBytecodeCompiler;
import de.hpi.swa.lox.parser.LoxParseError;
import de.hpi.swa.lox.runtime.LoxContext;
import de.hpi.swa.lox.runtime.LoxRuntimeError;

public abstract class LoadBuiltInNode extends BuiltInNodeWithArgs {

    public LoadBuiltInNode(LoxLanguage language) {
        super(language, 1);
    }

    @TruffleBoundary
    @Specialization
    static Object loadFile(TruffleString pathRelativeToWorkingDirectory,
            @Cached IndirectCallNode callNode,
            @Bind Node node,
            @Bind LoxLanguage language,
            @Bind LoxContext context) {
        var env = context.getEnv();
        var file = env.getPublicTruffleFile(pathRelativeToWorkingDirectory
                .toJavaStringUncached()).getAbsoluteFile();
        var cwd = env.getCurrentWorkingDirectory();
        env.setCurrentWorkingDirectory(file.getParent());
        try {
            var src = Source.newBuilder("lox", file).build();
            RootCallTarget rootTarget = LoxBytecodeCompiler.parseLox(language, src);
            return callNode.call(rootTarget);
        } catch (IOException e) {
            throw new LoxRuntimeError(e.getMessage(), node);
        } catch (LoxParseError e) {
            throw e;
        } finally {
            env.setCurrentWorkingDirectory(cwd);
        }
    }
}
