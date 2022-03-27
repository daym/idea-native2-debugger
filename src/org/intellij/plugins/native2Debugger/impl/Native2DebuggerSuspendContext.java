package org.intellij.plugins.native2Debugger.impl;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;

import java.util.List;
import java.util.Map;

public class Native2DebuggerSuspendContext extends XSuspendContext {
    private final Native2DebugProcess myDebuggerSession;
    private final List<Map.Entry<String, Object>> myGdbExecutionStack;

    public Native2DebuggerSuspendContext(Native2DebugProcess debuggerSession, List<Map.Entry<String, Object>> gdbExecutionStack) {
        myDebuggerSession = debuggerSession;
        myGdbExecutionStack = gdbExecutionStack;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
        // TODO: find execution stack of current thread
        return new Native2ExecutionStack(Native2DebuggerBundle.message("list.item.native2.frames"), myGdbExecutionStack, myDebuggerSession);
    }

    @Override
    public XExecutionStack /*@NotNull*/[] getExecutionStacks() {
        // TODO: print execution stacks of all threads
        return new XExecutionStack[]{
                getActiveExecutionStack(),
        };
    }

}
