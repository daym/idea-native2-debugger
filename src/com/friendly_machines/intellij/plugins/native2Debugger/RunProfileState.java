// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger;

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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.util.Key;
import com.intellij.util.ArrayUtil;
import com.pty4j.unix.Pty;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RunProfileState extends CommandLineState {
    public static final Key<RunProfileState> STATE = Key.create("STATE");
    public static final Key<Pty> PTY = Key.create("PTY");
    private final Configuration myConfiguration;
    private final TextConsoleBuilder myBuilder;
    private Pty myPty;

    public RunProfileState(Configuration configuration, ExecutionEnvironment environment, TextConsoleBuilder builder) {
        super(environment);
        myConfiguration = configuration;
        myBuilder = builder;
    }

    @Override
    @NotNull
    public ExecutionResult execute(@NotNull final Executor executor, @NotNull final ProgramRunner<?> runner) throws ExecutionException {
        final ProcessHandler processHandler = startProcess();
        final ConsoleView console = createConsole(executor); // keep this AFTER the startProcess call.
        if (console != null) {
            console.attachToProcess(processHandler);
        }
        DefaultExecutionResult q = new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
        return q;
    }

    @NotNull
    @Override
    protected OSProcessHandler startProcess() throws ExecutionException {
        try {
            myPty = new Pty(true, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionException(e);
        }
        String slaveName = myPty.getSlaveName();
        // myPty.getMasterFD()
        String gdbExecutableName = ProjectSettingsState.getInstance().gdbExecutableName;
        if (gdbExecutableName == null || gdbExecutableName.equals("")) {
            gdbExecutableName = "gdb";
        }

        GeneralCommandLine commandLine = new GeneralCommandLine(PathEnvironmentVariableUtil.findExecutableInWindowsPath(gdbExecutableName));
        commandLine.addParameter("-nw"); // no window
        commandLine.addParameter("-q");
        //commandLine.addParameter("-batch");
        //commandLine.addParameter("-return-child-result");
        // -d <sourcedir>
        // -s <symbols>
        // -cd=<dir>
        // -f (stack frame special format)
        // -tty=/dev/tty0
        //commandLine.addParameter("--interpreter=mi3");
        commandLine.addParameter("-ex");
        commandLine.addParameter("new-ui mi3 " + slaveName);
        //commandLine.setWorkDirectory(workingDirectory);
        //charset = EncodingManager.getInstance().getDefaultCharset();
        //final OSProcessHandler processHandler = creator.fun(commandLine);

        final OSProcessHandler osProcessHandler = new OSProcessHandler.Silent(commandLine);
        osProcessHandler.putUserData(STATE, this);
        osProcessHandler.putUserData(PTY, myPty);
        // "Since we cannot guarantee that the listener is added before process handled is start notified, ..." ugh
        // This assumes that we can do that still and have it have an effect. That's why we override execute() to make sure that that's the case.
        //myBuilder.addFilter(new Native2DebuggerGdbMiFilter(osProcessHandler, getEnvironment().getProject()));
        this.setConsoleBuilder(myBuilder);

        return osProcessHandler;
    }

    @Override
    protected AnAction /*@NotNull*/ [] createActions(ConsoleView console, ProcessHandler processHandler, Executor executor) {
        return ArrayUtil.append(super.createActions(console, processHandler, executor), new ToggleAction("frobnicate") {
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
