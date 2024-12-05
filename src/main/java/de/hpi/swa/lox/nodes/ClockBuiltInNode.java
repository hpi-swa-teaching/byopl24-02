package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.lox.runtime.data.LoxNumber;

public class ClockBuiltInNode extends BuiltInNode {
    @Override
    public LoxNumber execute(VirtualFrame frame) {
        return new LoxNumber((double) System.nanoTime() / 1_000_000_000.0);
    }
}
