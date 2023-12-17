package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.MemoryView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.intellij.xdebugger.memory.component.MemoryViewManager;
import org.jetbrains.annotations.NotNull;

public class ShowVariableInMemoryViewAction extends XDebuggerTreeActionBase {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    protected void perform(XValueNodeImpl node, @NotNull String nodeName, AnActionEvent e) {
        //var project = e.getProject() != null ? e.getProject() : ProjectManager.getInstance().getDefaultProject();
        final XDebugSession session = DebuggerUIUtil.getSession(e);
        if (session != null) {
            var process = session.getDebugProcess();
            var name = node.getName(); // TODO: Make more resilient
            var ui = session.getUI();
            var memoryViewContent = ui.findContent(MemoryViewManager.MEMORY_VIEW_CONTENT);
            if (memoryViewContent != null) {
                //ui.selectAndFocus(memoryView, true, false, false);
                var memoryView = (MemoryView) memoryViewContent.getComponent();
                memoryView.setAddressRange(String.format("&(%s)", nodeName));
            }
            //process.createConsole()
            // return ((DebugProcess)process).getDebuggerSession().getProcess()
        }
    }
    // TODO: override update() and set e.getPresenetation().setVisible()
}
