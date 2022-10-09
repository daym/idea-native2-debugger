// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.GdbOsProcessHandler;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public class RunProfileState extends CommandLineState {
    private final Configuration myConfiguration;
    private final TextConsoleBuilder myBuilder;

    public RunProfileState(Configuration configuration, ExecutionEnvironment environment, TextConsoleBuilder builder) {
        super(environment);
        myConfiguration = configuration;
        myBuilder = builder;
    }

    @NotNull
    @Override
    protected OSProcessHandler startProcess() throws ExecutionException {
//        try {
//            myPty = SystemInfo.isWindows ? new PtyOnlyWin() : new PtyOnlyUnix();
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new ExecutionException(e);
//        }
//        String slaveName = myPty.getSlaveName();
        // myPty.getMasterFD()
        String gdbExecutableName = ProjectSettingsState.getInstance().gdbExecutableName;
        if (gdbExecutableName == null || gdbExecutableName.equals("")) {
            gdbExecutableName = "gdb";
        }

//        if (SystemInfo.isWindows) {
//            GeneralCommandLine commandLine = new GeneralCommandLine(PathEnvironmentVariableUtil.findExecutableInWindowsPath("wsl.exe"));
//              commandLine.addParameter("gdbExecutableName"); // no window
//        } else
        //Pty: GeneralCommandLine commandLine = new PtyCommandLine(Arrays.stream(new String[] { PathEnvironmentVariableUtil.findExecutableInWindowsPath(gdbExecutableName) }).toList());
        GeneralCommandLine commandLine = new GeneralCommandLine(PathEnvironmentVariableUtil.findExecutableInWindowsPath(gdbExecutableName));
        // If using wsl.exe gdb ...
        //if (SystemInfo.isWindows) {
        //    commandLine.addParameter(gdbExecutableName);
        //}
        commandLine.addParameter("--nw"); // no window
        commandLine.addParameter("-q");
        //commandLine.addParameter("-batch");
        //commandLine.addParameter("-return-child-result");
        // -d <sourcedir>
        // -s <symbols>
        // -cd=<dir>
        // -f (stack frame special format)
        // -tty=/dev/tty0

        commandLine.addParameter("--interpreter=mi3");
        // gdb needs either forward-slashes or doubly-escaped backslashes
        //commandLine.addParameter("--tty=" + slaveName.replace("\\", "\\\\"));
        //commandLine.addParameter("--eval-command=new-ui mi3 " + slaveName.replace("\\", "\\\\"));

        //commandLine.setWorkDirectory(workingDirectory);
        //charset = EncodingManager.getInstance().getDefaultCharset();

        commandLine.setRedirectErrorStream(false);
        final OSProcessHandler osProcessHandler = new GdbOsProcessHandler(commandLine);
        // "Since we cannot guarantee that the listener is added before process handled is start notified, ..." ugh
        // This assumes that we can do that still and have it have an effect. That's why we override execute() to make sure that that's the case.
        //myBuilder.addFilter(new Native2DebuggerGdbMiFilter(osProcessHandler, getEnvironment().getProject()));
        this.setConsoleBuilder(myBuilder);

        return osProcessHandler;
    }

    @Override
    @NotNull
    public ExecutionResult execute(@NotNull final Executor executor, @NotNull final ProgramRunner<?> runner) throws ExecutionException {
        final ProcessHandler processHandler = startProcess();
        final ConsoleView console = createConsole(executor); // keep this AFTER the startProcess call.
        if (console != null) {
            console.attachToProcess(processHandler);
        }
        return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
    }

    @Override
    protected AnAction @NotNull[] createActions(ConsoleView console, ProcessHandler processHandler, Executor executor) {
        return ArrayUtil.append(super.createActions(console, processHandler, executor), new ToggleAction("frobnicate") {
            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT; // FIXME ?!
            }

            @Override
            public boolean isDumbAware() {
                return true;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return false;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean b) {

            }
        });
    }
}
