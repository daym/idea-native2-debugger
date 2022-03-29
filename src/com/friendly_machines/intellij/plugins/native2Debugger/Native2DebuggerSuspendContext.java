package com.friendly_machines.intellij.plugins.native2Debugger;

import com.friendly_machines.intellij.plugins.native2Debugger.impl.DebugProcess;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;

public class Native2DebuggerSuspendContext extends XSuspendContext {
    private final DebugProcess myDebuggerSession;
    private final Native2ExecutionStack[] myExecutionStacks;
    private final int myActiveStackId;

    public Native2DebuggerSuspendContext(DebugProcess debuggerSession, Native2ExecutionStack[] executionStacks, int activeStackId) {
        myDebuggerSession = debuggerSession;
        myExecutionStacks = executionStacks;
        myActiveStackId = activeStackId;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
        if (myActiveStackId >= 0 && myActiveStackId < myExecutionStacks.length) {
            return myExecutionStacks[myActiveStackId];
        } else {
            return null;
        }
    }

    @Override
    public XExecutionStack /*@NotNull*/[] getExecutionStacks() {
        // TODO: print execution stacks of all threads
        return myExecutionStacks;
    }
}
