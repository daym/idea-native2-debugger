package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.xdebugger.attach.XAttachProcessPresentationGroup;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AttachDebuggerAttachGroup implements XAttachProcessPresentationGroup {
    @Override
    public int getOrder() {
        return -15;
    }

    @Override
    public @NotNull @Nls String getGroupName() {
        return "Native2"; // FIXME translate
    }

    @Nls
    @Override
    public @NotNull String getItemDisplayText(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return info.getExecutableDisplayName();
    }

    @Override
    public @NotNull Icon getItemIcon(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return EmptyIcon.ICON_16;
    }
}
