package com.friendly_machines.intellij.plugins.native2Debugger;

import com.friendly_machines.intellij.plugins.native2Debugger.impl.Native2DebuggerGdbMiFilter;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.util.Key;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.DefaultExecutionResult;
import com.pty4j.unix.Pty;

import java.io.IOException;

public class Native2DebuggerRunProfileState extends CommandLineState {
    public static final Key<Native2DebuggerRunProfileState> STATE = Key.create("STATE");
    public static final Key<Pty> PTY = Key.create("PTY");
    private final Native2DebuggerConfiguration myConfiguration;
    private final TextConsoleBuilder myBuilder;
    private Pty myPty;

    public Native2DebuggerRunProfileState(Native2DebuggerConfiguration configuration, ExecutionEnvironment environment, TextConsoleBuilder builder) {
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
        return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
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
        System.err.println("PTY SLAVE " + slaveName);
        // myPty.getMasterFD()
        GeneralCommandLine commandLine = new GeneralCommandLine(PathEnvironmentVariableUtil.findExecutableInWindowsPath("gdb"));
        commandLine.addParameter("-nw"); // no window
        commandLine.addParameter("-q");
        commandLine.addParameter("-return-child-result");
        // -d <sourcedir>
        // -s <symbols>
        // -cd=<dir>
        // -f (stack frame special format)
        // -tty=/dev/tty0
        //commandLine.addParameter("--interpreter=mi3");
        commandLine.addParameter("-ex");
        commandLine.addParameter("new-ui mi3 " + slaveName);
        //commandLine.addParameter("--args");
        //commandLine.addParameter("./target/debug/amd-host-image-builder"); // FIXME
        //commandLine.setWorkDirectory(workingDirectory);
        //charset = EncodingManager.getInstance().getDefaultCharset();
        //final OSProcessHandler processHandler = creator.fun(commandLine);

        final OSProcessHandler osProcessHandler = new OSProcessHandler(commandLine);
        osProcessHandler.putUserData(STATE, this);
        osProcessHandler.putUserData(PTY, myPty);
        // "Since we cannot guarantee that the listener is added before process handled is start notified, ..." ugh
        // This assumes that we can do that still and have it have an effect. That's why we override execute() to make sure that that's the case.
        //myBuilder.addFilter(new Native2DebuggerGdbMiFilter(osProcessHandler, getEnvironment().getProject()));
        this.setConsoleBuilder(myBuilder);

        return osProcessHandler;
    }

}
