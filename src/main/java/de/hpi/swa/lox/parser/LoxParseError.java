package de.hpi.swa.lox.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

@ExportLibrary(InteropLibrary.class)
public class LoxParseError extends AbstractTruffleException {
    public static final long serialVersionUID = 1L;
    private final Source source;
    private final int line;
    private final int column;
    private final int length;

    public LoxParseError(Source source, int line, int column, int length, String message) {
        super(message);
        this.source = source;
        this.line = line;
        this.column = column;
        this.length = length;
    }

    @ExportMessage
    ExceptionType getExceptionType() {
        return ExceptionType.PARSE_ERROR;
    }

    @ExportMessage
    boolean hasSourceLocation() {
        return source != null;
    }

    @ExportMessage(name = "getSourceLocation")
    @TruffleBoundary
    SourceSection getSourceSection() throws UnsupportedMessageException {
        if (source == null) {
            throw UnsupportedMessageException.create();
        }
        return source.createSection(line, column, length);
    }

    public static LoxParseError build(
            Source source, ParseTree tree, String message) {
        String s = message;
        var line = 0;
        var column = 0;
        var length = 0;
        if (tree.getPayload() instanceof ParserRuleContext context) {
            Token startToken = context.getStart();
            Token stopToken = context.getStop();
            line = startToken.getLine();
            column = startToken.getCharPositionInLine();
            length = stopToken.getStopIndex() - startToken.getStartIndex() + 1;
            s = formatMessage(formatLocation(line, column), message);
        }
        return new LoxParseError(
                source,
                line,
                column,
                length,
                s);
    }

    @TruffleBoundary
    private static String formatMessage(String location, String message) {
        return String.format("Error(s) parsing script:%n" + location + message);
    }

    @TruffleBoundary
    private static String formatLocation(int line, int column) {
        return String.format("-- line %d col %d: ", line, column);
    }

}