// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.attach.XAttachDebugger;
import com.intellij.xdebugger.attach.XAttachHost;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import com.friendly_machines.intellij.plugins.ideanative2debugger.Runner;

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
        /* # Lowlevel
         *
         * GeneralCommandLine commandLine = new GeneralCommandLine("your", "command", "here");
         * ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
         * OSProcessHandler processHandler = new OSProcessHandler(commandLine);
         * consoleView.attachToProcess(processHandler);
         * consoleView.print("Executing command: " + commandLine.getCommandLineString(), ConsoleViewContentType.NORMAL_OUTPUT);
         * processHandler.startNotify();
         */
        var runManager = RunManager.getInstance(project);
        var configurationType = ConfigurationType.getInstance();
        var configurationFactory = configurationType.getConfigurationFactories()[0]; // TODO: Have an extra factory here
        var settings = runManager.createConfiguration("Attach to Process", configurationFactory);
        var configuration = (Configuration) settings.getConfiguration();
        configuration.setAttachTarget(Integer.toString(processInfo.getPid())); // FIXME: Test

        var executor = DefaultDebugExecutor.getDebugExecutorInstance();
        var runner = new Runner();
        var environment = new ExecutionEnvironment(executor, runner, settings, project);
        runner.execute(environment);
    }
}
