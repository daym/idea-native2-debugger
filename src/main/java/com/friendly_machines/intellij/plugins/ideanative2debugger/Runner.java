// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public class Runner implements ProgramRunner<RunnerSettings> {
    private static final String RUNNER_ID = "Native2DebuggerRunner";
    static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    public Runner() {
        super();
    }

    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }

    /**
     * This makes sure the Debug mode is executed and not run mode
     *
     * @param executorId DefaultDebugExecutor.EXECUTOR_ID or Run executor
     * @param profile    The profile
     */
    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        //System.err.println("native canRun executor: " + executorId + ", profile: " + profile + " " + profile.getClass());
        return (DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)); // && profile instanceof Configuration);
    }

    @Nullable
    protected RunContentDescriptor createContentDescriptor(com.intellij.execution.configurations.RunProfileState runProfileState, ExecutionEnvironment environment)
            throws ExecutionException {
        XDebugSession debugSession =
                XDebuggerManager.getInstance(environment.getProject()).startSession(environment, new XDebugProcessStarter() {
                    @Override
                    public @NotNull
                    XDebugProcess start(final @NotNull XDebugSession session) throws ExecutionException {
                        ACTIVE.set(Boolean.TRUE); // FIXME
                        try {
                            var execArguments = new String[0];
                            @Nullable String attachTarget = null;
                            //if (runProfileState instanceof RunProfileState) {
                            if (runProfileState instanceof org.rust.cargo.runconfig.CargoRunState) {
                                var state = (org.rust.cargo.runconfig.CargoRunState) runProfileState;
                                // TODO: Stick gdb into "cargo run" command line:
                                // cargo --config "target.'cfg(unix)'.runner = 'gdb --args x'" run
                                // TODO check how command line patches work state.prepareCommandLine()
                            } else {
                                var state = (RunProfileState) runProfileState;
                                execArguments = state.getExecArguments();
                                attachTarget = state.getAttachTarget();
                            }
                            final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), Runner.this);

                            assert executionResult != null;
                            return new DebugProcess(environment, executionResult, session, execArguments, attachTarget);
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new ExecutionException(e);
                        } finally {
                            ACTIVE.remove();
                        }
                    }
                });
        return debugSession.getRunContentDescriptor();
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, Objects.requireNonNull(environment.getState()), state -> {
            //FileDocumentManager.getInstance().saveAllDocuments();
            return createContentDescriptor(state, environment);
        });
    }
}