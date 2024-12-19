package de.hpi.swa.lox.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import de.hpi.swa.lox.LoxLanguage;

@NodeInfo(language = "lox", description = "The root of all Lox execution trees")
public abstract class LoxRootNode extends RootNode {

    public String name;

    public LoxRootNode(LoxLanguage lang,
            FrameDescriptor frameDescriptor) {
        super(lang, frameDescriptor);
        name = "unnamed";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "root " + getName();
    }
}
