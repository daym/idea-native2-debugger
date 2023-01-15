package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
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
import java.awt.*;

public class ThrowedCatchpointType extends XBreakpointType<XBreakpoint<ThrowedCatchpointProperties>, ThrowedCatchpointProperties> {
    protected ThrowedCatchpointType() {
        super("native2-throwed-catchpoint", "'throw' catchpoint");
    }

    protected ThrowedCatchpointType(@NonNls @NotNull String id, @Nls @NotNull String title, boolean suspendThreadSupported) {
        super(id, title, suspendThreadSupported);
    }

    @Override
    public @Nls String getDisplayText(XBreakpoint<ThrowedCatchpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        if (properties != null) {
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
    public ThrowedCatchpointProperties createProperties() {
        return new ThrowedCatchpointProperties(".*");
    }

    @Override
    public boolean isAddBreakpointButtonVisible() {
        return true;
    }

    @Override
    public XBreakpoint<ThrowedCatchpointProperties> addBreakpoint(final Project project, JComponent parentComponent) {
        // dialog...
        //dialog.showDialog();
        //dialog.getSelected()

        // on ok
//        return WriteAction.compute(() -> XDebuggerManager.getInstance(project).getBreakpointManager()
//                .addBreakpoint(this, new PyExceptionBreakpointProperties(qualifiedName)));
        return null; // FIXME
    }

//    @Override
//    public String getBreakpointsDialogHelpTopic() {
//        return "reference.dialogs.breakpoints";
//    }

    @Nullable
    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XBreakpoint<ThrowedCatchpointProperties> breakpoint,
                                                       @NotNull Project project) {
        return null; // TODO: PyDebuggerEditorsProvider();
    }

    private static ThrowedCatchpointProperties createDefaultBreakpointProperties() {
        var p = new ThrowedCatchpointProperties(".*");
        return p;
    }

    @Override
    public XBreakpoint<ThrowedCatchpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<ThrowedCatchpointProperties> creator) {
        final XBreakpoint<ThrowedCatchpointProperties> breakpoint = creator.createBreakpoint(createDefaultBreakpointProperties());
        breakpoint.setEnabled(true);
        return breakpoint;
    }


    @Override
    public XBreakpointCustomPropertiesPanel<XBreakpoint<ThrowedCatchpointProperties>> createCustomPropertiesPanel(@NotNull Project project
    ) {
        return null;
    }

}
