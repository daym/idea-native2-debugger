package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WatchpointType extends XBreakpointType<XBreakpoint<WatchpointProperties>, WatchpointProperties> {
    protected WatchpointType() {
        super("native2-watchpoint", "Watchpoints");
    }

    protected WatchpointType(@NonNls @NotNull String id, @Nls @NotNull String title, boolean suspendThreadSupported) {
        super(id, title, suspendThreadSupported);
    }

    @Override
    public @Nls String getDisplayText(XBreakpoint<WatchpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        if (properties != null) {
            var subject = properties.myExpression;
            return subject;
        } else {
            return "?";
        }
    }

    @NotNull
    @Override
    public Icon getEnabledIcon() {
        return AllIcons.Debugger.Db_exception_breakpoint;
    }

    @NotNull
    @Override
    public Icon getDisabledIcon() {
        return AllIcons.Debugger.Db_disabled_exception_breakpoint;
    }

    @Override
    public WatchpointProperties createProperties() { // FIXME
        return new WatchpointProperties("", false, true);
    }

    @Override
    public boolean isAddBreakpointButtonVisible() {
        return true;
    }

    @Override
    public XBreakpoint<WatchpointProperties> addBreakpoint(final Project project, JComponent parentComponent) {
        final var dialog = new WatchpointAddingDialog(project);
        if (!dialog.showAndGet()) {
            return null;
        }

        return WriteAction.compute(() -> XDebuggerManager.getInstance(project).getBreakpointManager()
                .addBreakpoint(this, new WatchpointProperties(dialog.getExpression(), dialog.isMonitoringReads(), dialog.isMonitoringWrites())));
    }

//    @Override
//    public String getBreakpointsDialogHelpTopic() {
//        return "reference.dialogs.breakpoints";
//    }

    @Nullable
    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XBreakpoint<WatchpointProperties> breakpoint,
                                                       @NotNull Project project) {
        return null; // TODO: DebuggerEditorsProvider();
    }

    @Override
    public XBreakpoint<WatchpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<WatchpointProperties> creator) {
        return null;
    }

    @Override
    public XBreakpointCustomPropertiesPanel<XBreakpoint<WatchpointProperties>> createCustomPropertiesPanel(@NotNull Project project
    ) {
        return null;
    }
}
