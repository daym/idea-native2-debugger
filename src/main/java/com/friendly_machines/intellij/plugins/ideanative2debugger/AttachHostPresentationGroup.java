package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.attach.XAttachPresentationGroup;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AttachHostPresentationGroup implements XAttachPresentationGroup<AttachHost>  { // <? extends XAttachHost>
    public static final AttachHostPresentationGroup INSTANCE = new AttachHostPresentationGroup();
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public @NotNull @Nls String getGroupName() {
        return "Gdb/Mi-proxied hosts";
    }

    @Override
    public @NotNull Icon getProcessIcon(@NotNull Project project, @NotNull AttachHost info, @NotNull UserDataHolder dataHolder) {
        return AllIcons.Icons.Ide.MenuArrow;
    }

    @Override
    public @NotNull @Nls String getProcessDisplayText(@NotNull Project project, @NotNull AttachHost info, @NotNull UserDataHolder dataHolder) {
        return "Gdb/Mi-proxied host";
    }

    @Override
    public int compare(AttachHost t1, AttachHost t2) {
        return t1.compareTo(t2);
    }
}
