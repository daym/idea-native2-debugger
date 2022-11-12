// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.attach.XAttachDebugger;
import com.intellij.xdebugger.attach.XAttachHost;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class AttachDebugger implements XAttachDebugger {
    public AttachDebugger(Project project, XAttachHost xAttachHost, ProcessInfo processInfo, UserDataHolder userDataHolder) {
    }

    @Override
    public @NotNull
    @Nls String getDebuggerDisplayName() {
        return "Native2 attach debugger";
    }

    @Override
    public void attachDebugSession(@NotNull Project project, @NotNull XAttachHost xAttachHost, @NotNull ProcessInfo processInfo) throws ExecutionException {
        // FIXME
        //var runner = new Runnner();
        // var configuration = new Configuration(project, factory);
        //var state = new RunProfileState(configuration, environment, console builder);
        // FIXME runner.execute(projec);
        // FIXME: How to create an execution environment
        //final ExecutionResult result = state.execute(state.getEnvironment().getExecutor(), this);
    }
}
