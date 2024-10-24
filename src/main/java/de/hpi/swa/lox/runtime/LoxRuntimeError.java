package de.hpi.swa.lox.runtime;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;

@ExportLibrary(InteropLibrary.class)
public class LoxRuntimeError extends AbstractTruffleException {
    public LoxRuntimeError(String message, Node node) {
        super(message, node);
    }

    @ExportMessage
    ExceptionType getExceptionType() {
        return ExceptionType.RUNTIME_ERROR;
    }
}
