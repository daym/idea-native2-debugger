package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.attach.XAttachHost;
import com.intellij.xdebugger.attach.XAttachHostProvider;
import com.intellij.xdebugger.attach.XAttachPresentationGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// TODO: gdb target remote, gdb target extended-remote (the latter better for us); via serial, socket

public class AttachHostProvider implements XAttachHostProvider {
    @Override
    public @NotNull XAttachPresentationGroup<? extends XAttachHost> getPresentationGroup() {
        return AttachHostPresentationGroup.INSTANCE;
    }

    @Override
    public @NotNull List getAvailableHosts(@Nullable Project project) {
        return List.of(new AttachHost[] { new AttachHost("default") }); // FIXME
    }
}
