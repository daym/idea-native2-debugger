package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.ShlibCatchpointAddingDialog;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.ShlibCatchpointCatchType;
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

public class ShlibCatchpointType extends XBreakpointType<XBreakpoint<ShlibCatchpointProperties>, ShlibCatchpointProperties> {
    protected ShlibCatchpointType() {
        super("native2-shlib-catchpoint", "Shared Library Manager Breakpoints");
    }

    protected ShlibCatchpointType(@NonNls @NotNull String id, @Nls @NotNull String title, boolean suspendThreadSupported) {
        super(id, title, suspendThreadSupported);
    }

    @Override
    public @Nls String getDisplayText(XBreakpoint<ShlibCatchpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        if (properties != null) {
            var subject = properties.myLibraryNameRegexp;
            return switch (properties.myCatchType) {
                case Load -> "Loading of " + subject; // TODO: i18n
                case Unload -> "Unloading of " + subject;
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
    public ShlibCatchpointProperties createProperties() { // FIXME
        return new ShlibCatchpointProperties(ShlibCatchpointCatchType.Load, "");
    }

    @Override
    public boolean isAddBreakpointButtonVisible() {
        return true;
    }

    @Override
    public XBreakpoint<ShlibCatchpointProperties> addBreakpoint(final Project project, JComponent parentComponent) {
        final var dialog = new ShlibCatchpointAddingDialog(project);
        if (!dialog.showAndGet()) {
            return null;
        }

        return WriteAction.compute(() -> XDebuggerManager.getInstance(project).getBreakpointManager()
                .addBreakpoint(this, new ShlibCatchpointProperties(dialog.getCatchpointType().get(), dialog.getLibraryNameRegexp())));
    }

//    @Override
//    public String getBreakpointsDialogHelpTopic() {
//        return "reference.dialogs.breakpoints";
//    }

    @Nullable
    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XBreakpoint<ShlibCatchpointProperties> breakpoint,
                                                       @NotNull Project project) {
        return null; // TODO: DebuggerEditorsProvider();
    }
    @Override
    public XBreakpoint<ShlibCatchpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<ShlibCatchpointProperties> creator) {
        return null;
    }
    @Override
    public XBreakpointCustomPropertiesPanel<XBreakpoint<ShlibCatchpointProperties>> createCustomPropertiesPanel(@NotNull Project project
    ) {
        return null;
    }
}
