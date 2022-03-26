package org.intellij.plugins.native2Debugger;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;

public class Native2DebuggerSession {
    private final Native2DebugProcess myProcess;

    public Native2DebuggerSession(Native2DebugProcess process) {
        myProcess = process;
    }

    public Debugger.State getCurrentState() {
        // FIXME
        return Debugger.State.SUSPENDED;
    }
}
