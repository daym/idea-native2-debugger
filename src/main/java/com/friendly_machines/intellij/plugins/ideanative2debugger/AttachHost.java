package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.xdebugger.attach.XAttachHost;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO: info os processes, info os procgroups, info os threads
public class AttachHost implements XAttachHost, Comparable<AttachHost> {
    private final String key;

    @Override
    public @NotNull List<ProcessInfo> getProcessList() throws ExecutionException {
        return List.of(new ProcessInfo[] { new ProcessInfo(1, "cmdline", "execname", "args", "execpath") });
    }
    public AttachHost(String key) {
        this.key = key;
    }

    @Override
    public int compareTo(@NotNull AttachHost other) {
        return this.key.compareTo(other.key);
    }
}
