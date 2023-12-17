// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//import org.rust.cargo.toolchain.CargoCommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                            var state = (RunProfileState) runProfileState;
                            execArguments = state.getExecArguments();
                            attachTarget = state.getAttachTarget();
                            final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), Runner.this);

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