package com.friendly_machines.intellij.plugins.ideanative2debugger;

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

import com.intellij.icons.AllIcons;

import javax.swing.*;

public class ThrownCatchpointType extends XBreakpointType<XBreakpoint<ThrownCatchpointProperties>, ThrownCatchpointProperties> {
    private final String ANY_EXCEPTION = ".*";
    protected ThrownCatchpointType() {
        super("native2-throwed-catchpoint", "C++ Exception Thrown Breakpoints");
    }

    protected ThrownCatchpointType(@NonNls @NotNull String id, @Nls @NotNull String title, boolean suspendThreadSupported) {
        super(id, title, suspendThreadSupported);
    }

    @Override
    public @Nls String getDisplayText(XBreakpoint<ThrownCatchpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        if (properties != null) {
            if (ANY_EXCEPTION.equals(properties.myExceptionRegexp))
                return "Any exception";
            else
                return properties.myExceptionRegexp;
        } else {
            return "throw()";
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
    public ThrownCatchpointProperties createProperties() {
        return new ThrownCatchpointProperties(ANY_EXCEPTION);
    }

    @Override
    public boolean isAddBreakpointButtonVisible() {
        return true;
    }

    @Override
    public XBreakpoint<ThrownCatchpointProperties> addBreakpoint(final Project project, JComponent parentComponent) {
        // dialog...
        //dialog.showDialog();
        //dialog.getSelected()

        // on ok
        return WriteAction.compute(() -> XDebuggerManager.getInstance(project).getBreakpointManager()
                .addBreakpoint(this, new ThrownCatchpointProperties("qq")));
    }

//    @Override
//    public String getBreakpointsDialogHelpTopic() {
//        return "reference.dialogs.breakpoints";
//    }

    @Nullable
    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XBreakpoint<ThrownCatchpointProperties> breakpoint,
                                                       @NotNull Project project) {
        return null; // TODO: DebuggerEditorsProvider();
    }

    private static ThrownCatchpointProperties createDefaultBreakpointProperties() {
        var p = new ThrownCatchpointProperties(".*");
        return p;
    }

    @Override
    public XBreakpoint<ThrownCatchpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<ThrownCatchpointProperties> creator) {
        final XBreakpoint<ThrownCatchpointProperties> breakpoint = creator.createBreakpoint(createDefaultBreakpointProperties());
        breakpoint.setEnabled(true);
        return breakpoint;
    }


    @Override
    public XBreakpointCustomPropertiesPanel<XBreakpoint<ThrownCatchpointProperties>> createCustomPropertiesPanel(@NotNull Project project
    ) {
        return null;
    }
}
