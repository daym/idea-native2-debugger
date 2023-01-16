package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.AdaCatchpointCatchType;
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

public class AdaCatchpointType extends XBreakpointType<XBreakpoint<AdaCatchpointProperties>, AdaCatchpointProperties> {
    private static final String UNHANDLED_EXCEPTION = "";
    protected AdaCatchpointType() {
        super("native2-ada-catchpoint", "Ada Exception Breakpoints");
    }

    protected AdaCatchpointType(@NonNls @NotNull String id, @Nls @NotNull String title, boolean suspendThreadSupported) {
        super(id, title, suspendThreadSupported);
    }

    @Override
    public @Nls String getDisplayText(XBreakpoint<AdaCatchpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        if (properties != null) {
            var subject = (UNHANDLED_EXCEPTION.equals(properties.myException)) ? DebuggerBundle.message("debugger.exception.breakpoint.unhandled.exception") : properties.myException;
            return switch (properties.myCatchType) {
                case Exception -> "Exception for " + subject;
                case Handlers -> "Handlers for " + subject;
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
    public AdaCatchpointProperties createProperties() {
        return new AdaCatchpointProperties(AdaCatchpointCatchType.Exception, UNHANDLED_EXCEPTION);
    }

    @Override
    public boolean isAddBreakpointButtonVisible() {
        return true;
    }

    @Override
    public XBreakpoint<AdaCatchpointProperties> addBreakpoint(final Project project, JComponent parentComponent) {
        final var dialog = new AdaCatchpointAddingDialog(project);
        if (!dialog.showAndGet()) {
            return null;
        }

        return WriteAction.compute(() -> XDebuggerManager.getInstance(project).getBreakpointManager()
                .addBreakpoint(this, new AdaCatchpointProperties(dialog.getCatchpointType().get(), dialog.getException())));
    }

//    @Override
//    public String getBreakpointsDialogHelpTopic() {
//        return "reference.dialogs.breakpoints";
//    }

    @Nullable
    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XBreakpoint<AdaCatchpointProperties> breakpoint,
                                                       @NotNull Project project) {
        return null; // TODO: DebuggerEditorsProvider();
    }

    private static AdaCatchpointProperties createDefaultBreakpointProperties() {
        var p = new AdaCatchpointProperties(AdaCatchpointCatchType.Exception, UNHANDLED_EXCEPTION);
        return p;
    }

    @Override
    public XBreakpoint<AdaCatchpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<AdaCatchpointProperties> creator) {
        final XBreakpoint<AdaCatchpointProperties> breakpoint = creator.createBreakpoint(createDefaultBreakpointProperties());
        breakpoint.setEnabled(true);
        return breakpoint;
    }


    @Override
    public XBreakpointCustomPropertiesPanel<XBreakpoint<AdaCatchpointProperties>> createCustomPropertiesPanel(@NotNull Project project
    ) {
        return null;
    }
}
