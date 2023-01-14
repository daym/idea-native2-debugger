package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.GdbMiOperationException;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.XDebuggerActionBase;
import com.intellij.xdebugger.impl.actions.XDebuggerSuspendedActionHandler;
import org.jetbrains.annotations.NotNull;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;

import java.io.IOException;

/// Action that maps to immediate return
public class ReverseStepOverAction extends XDebuggerActionBase {
    private final XDebuggerSuspendedActionHandler myHandler;
    @Override
    protected @NotNull DebuggerActionHandler getHandler(@NotNull DebuggerSupport debuggerSupport) {
        return myHandler;
    }
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    public ReverseStepOverAction() {
        myHandler = new XDebuggerSuspendedActionHandler() {
            @Override
            protected void perform(@NotNull XDebugSession session, DataContext dataContext) {
                final XDebugProcess debugProcess = session.getDebugProcess();
                if (debugProcess instanceof DebugProcess) {
                    var process = (DebugProcess) debugProcess;
                    try {
                        process.next(true);
                    } catch (GdbMiOperationException | IOException e) {
                        throw new RuntimeException(e); // FIXME dialog box?
                    } catch (InterruptedException e) {
                        // pucgenie: Most probably interrupted by the IDE.
                        e.printStackTrace();
                        //throw new RuntimeException(e);
                    }
                }
            }

            @Override
            protected boolean isEnabled(@NotNull XDebugSession session, DataContext dataContext) {
                final XDebugProcess debugProcess = session.getDebugProcess();
                if (debugProcess instanceof DebugProcess) {
                    var process = (DebugProcess) debugProcess;
                    if (!process.hasRecording()) {
                        return false;
                    }
                }
                return super.isEnabled(session, dataContext);
            }
        };

    }
}
