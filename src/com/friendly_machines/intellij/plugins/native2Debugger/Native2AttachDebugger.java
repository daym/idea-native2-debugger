package com.friendly_machines.intellij.plugins.native2Debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.attach.XAttachDebugger;
import com.intellij.xdebugger.attach.XAttachHost;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class Native2AttachDebugger implements XAttachDebugger {
    public Native2AttachDebugger(Project project, XAttachHost xAttachHost, ProcessInfo processInfo, UserDataHolder userDataHolder) {
    }

    @Override
    public @NotNull
    @Nls String getDebuggerDisplayName() {
        return "NATIVE2 debugger";
    }

    @Override
    public void attachDebugSession(@NotNull Project project, @NotNull XAttachHost xAttachHost, @NotNull ProcessInfo processInfo) throws ExecutionException {
        // FIXME
        /*PyAttachToProcessDebugRunner runner = new PyAttachToProcessDebugRunner(project, processInfo.getPid(), mySdkHome);
        runner.launch();*/
        Native2DebuggerRunner runner = new Native2DebuggerRunner();
        // FIXME runner.execute(projec);
        // FIXME: How to create an execution environment
        //CommandLineState state = CommandLineState.create(myProject, mySdkPath, serverSocket.getLocalPort(), myPid);
        //final ExecutionResult result = state.execute(state.getEnvironment().getExecutor(), this);
    }
}
