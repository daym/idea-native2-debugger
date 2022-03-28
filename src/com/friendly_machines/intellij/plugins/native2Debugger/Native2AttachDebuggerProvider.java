package com.friendly_machines.intellij.plugins.native2Debugger;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.attach.XAttachDebugger;
import com.intellij.xdebugger.attach.XAttachDebuggerProvider;
import com.intellij.xdebugger.attach.XAttachHost;
import org.jetbrains.annotations.NotNull;
import com.intellij.xdebugger.attach.LocalAttachHost;

import java.util.ArrayList;
import java.util.List;

public class Native2AttachDebuggerProvider implements XAttachDebuggerProvider {
    @Override
    public boolean isAttachHostApplicable(@NotNull XAttachHost xAttachHost) {
        return xAttachHost instanceof LocalAttachHost;
    }

    @Override
    public @NotNull List<XAttachDebugger> getAvailableDebuggers(@NotNull Project project, @NotNull XAttachHost xAttachHost, @NotNull ProcessInfo processInfo, @NotNull UserDataHolder userDataHolder) {
        List<XAttachDebugger> result = new ArrayList<XAttachDebugger>();
        result.add(new Native2AttachDebugger(project, xAttachHost, processInfo, userDataHolder));
        return result;
    }
}
