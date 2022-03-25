// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
//import org.intellij.lang.xpath.native2.run.Native2CommandLineState;
//import org.intellij.lang.xpath.native2.run.Native2RunConfiguration;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Native2DebuggerRunner implements ProgramRunner<RunnerSettings> {
  static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

  @Override
  public @NotNull String getRunnerId() {
    return "Native2DebuggerRunner";
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    System.err.println("HELLO WORLD");
    return true;
    // FIXME: return executorId.equals("Debug") && profile instanceof Native2RunConfiguration;
  }

  private RunContentDescriptor createContentDescriptor(RunProfileState runProfileState, @NotNull ExecutionEnvironment environment)
          throws ExecutionException {
    XDebugSession debugSession =
            XDebuggerManager.getInstance(environment.getProject()).startSession(environment, new XDebugProcessStarter() {
              @Override
              public @NotNull XDebugProcess start(final @NotNull XDebugSession session) throws ExecutionException {
                ACTIVE.set(Boolean.TRUE);
                try {
                  //final Native2CommandLineState c = (Native2CommandLineState)runProfileState;
                  final ExecutionResult result = runProfileState.execute(environment.getExecutor(), Native2DebuggerRunner.this);
                  return new Native2DebugProcess(session, result); // , c.getExtensionData().getUserData(Native2DebuggerExtension.VERSION));
                }
                finally {
                  ACTIVE.remove();
                }
              }
            });
    return debugSession.getRunContentDescriptor();
  }

  @Override
  public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
    System.err.println("HELLO WORLD 3");
    ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, Objects.requireNonNull(environment.getState()), state -> {
      FileDocumentManager.getInstance().saveAllDocuments();
      return createContentDescriptor(state, environment);
    });
  }
}