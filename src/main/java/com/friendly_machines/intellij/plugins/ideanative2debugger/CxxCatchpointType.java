package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.CxxCatchpointCatchType;
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

public class CxxCatchpointType extends XBreakpointType<XBreakpoint<CxxCatchpointProperties>, CxxCatchpointProperties> {
    private static final String ANY_EXCEPTION = ".*";
    protected CxxCatchpointType() {
        super("native2-cxx-catchpoint", "C++ Exception Breakpoints");
    }

    protected CxxCatchpointType(@NonNls @NotNull String id, @Nls @NotNull String title, boolean suspendThreadSupported) {
        super(id, title, suspendThreadSupported);
    }

    @Override
    public @Nls String getDisplayText(XBreakpoint<CxxCatchpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        if (properties != null) {
            var subject = (ANY_EXCEPTION.equals(properties.myExceptionRegexp)) ? "any exception" : properties.myExceptionRegexp;
            return switch (properties.myCatchType) {
                case Throw -> "Throw for " + subject;
                case Rethrow -> "Rethrow for " + subject;
                case Catch -> "Catch handler for " + subject;
            };
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
    public CxxCatchpointProperties createProperties() {
        return new CxxCatchpointProperties(CxxCatchpointCatchType.Throw, ANY_EXCEPTION);
    }

    @Override
    public boolean isAddBreakpointButtonVisible() {
        return true;
    }

    @Override
    public XBreakpoint<CxxCatchpointProperties> addBreakpoint(final Project project, JComponent parentComponent) {
        // FIXME: Show dialog with the possible CatchpointCatchType and a regexp field here...
        //dialog.showDialog();
        //dialog.getSelected()

        // on ok
        return WriteAction.compute(() -> XDebuggerManager.getInstance(project).getBreakpointManager()
                .addBreakpoint(this, new CxxCatchpointProperties(CxxCatchpointCatchType.Throw, "qq")));
    }

//    @Override
//    public String getBreakpointsDialogHelpTopic() {
//        return "reference.dialogs.breakpoints";
//    }

    @Nullable
    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XBreakpoint<CxxCatchpointProperties> breakpoint,
                                                       @NotNull Project project) {
        return null; // TODO: DebuggerEditorsProvider();
    }

    private static CxxCatchpointProperties createDefaultBreakpointProperties() {
        var p = new CxxCatchpointProperties(CxxCatchpointCatchType.Throw, ANY_EXCEPTION);
        return p;
    }

    @Override
    public XBreakpoint<CxxCatchpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<CxxCatchpointProperties> creator) {
        final XBreakpoint<CxxCatchpointProperties> breakpoint = creator.createBreakpoint(createDefaultBreakpointProperties());
        breakpoint.setEnabled(true);
        return breakpoint;
    }


    @Override
    public XBreakpointCustomPropertiesPanel<XBreakpoint<CxxCatchpointProperties>> createCustomPropertiesPanel(@NotNull Project project
    ) {
        return null;
    }
}
