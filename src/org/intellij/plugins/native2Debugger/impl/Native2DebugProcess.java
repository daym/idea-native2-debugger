// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.intellij.plugins.native2Debugger.*;
import org.intellij.plugins.native2Debugger.rt.engine.Breakpoint;
import org.intellij.plugins.native2Debugger.rt.engine.BreakpointManager;
import org.intellij.plugins.native2Debugger.rt.engine.BreakpointManagerImpl;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

// TODO: -break-insert, -break-condition, -break-list, -break-delete, -break-disable, -break-enable, -dprintf-insert (!), -break-passcount, -break-watch, -catch-load
// TODO: -environment-cd, -environment-directory, -environment-pwd
// TODO: -thread-info, -thread-list-ids, -thread-select
// TODO: -stack-info-frame, -stack-list-arguments, -stack-list-frames, -stack-list-locals, -stack-list-variables, -stack-select-frame,
// TODO: fixed variable object, floating variable object, -var-create, -var-delete, -var-info-type, -var-info-expression, -var-info-path-expression, -var-show-attributes, -var-evaluate-expression, -var-assign, -var-update, -var-set-frozen, -var-set-update-range
// TODO: -data-read-memory-bytes, -data-write-memory-bytes
// TODO: tracepoints, -trace-find, -trace-define-variable, -trace-frame-collected, -trace-list-variables, -trace-start, -trace-save
// TODO: registerAdditionalActions(DefaultActionGroup leftToolbar, DefaultActionGroup topToolbar, DefaultActionGroup settings) ?
// TODO: public XValueMarkerProvider<?,?> createValueMarkerProvider(); If debugger values have unique ids just return these ids from getMarker(XValue) method. Alternatively implement markValue(XValue) to store a value in some registry and implement unmarkValue(XValue, Object) to remote it from the registry. In such a case the getMarker(XValue) method can return null if the value isn't marked.

// ?: -symbol-info-functions, -symbol-info-module-functions, -symbol-info-module-variables, -symbol-info-modules, -symbol-info-types, -symbol-info-variables, -symbol-list-lines

// See <https://dploeger.github.io/intellij-api-doc/com/intellij/xdebugger/XDebugProcess.html>
public class Native2DebugProcess extends XDebugProcess implements Disposable {
  private static final Key<Native2DebugProcess> KEY = Key.create("PROCESS");

  private final Native2DebuggerEditorsProvider myEditorsProvider;
  private final ProcessHandler myProcessHandler;
  private final ExecutionConsole myExecutionConsole;
  private final OutputStream myChildIn;

  private BreakpointManager myBreakpointManager = new BreakpointManagerImpl();

  private final XBreakpointHandler<?>[] myXBreakpointHandlers = new XBreakpointHandler<?>[]{
    new Native2BreakpointHandler(this, Native2BreakpointType.class),
  };
  private Native2DebuggerSession myDebuggerSession;

  void send(String operation, String... options) {
    try {
      myChildIn.write(operation.getBytes(StandardCharsets.UTF_8));
      for (String option: options) {
        myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
        myChildIn.write(option.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
      }
      myChildIn.write("\n".getBytes(StandardCharsets.UTF_8));
      myChildIn.flush();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  String fileLineReference(XSourcePosition position) {
    return position.getFile().getPath() + ":" + (position.getLine() + 1);
  }

  public Native2DebugProcess(Native2DebuggerRunProfileState runProfileState, ExecutionEnvironment environment, Native2DebuggerRunner runner, XDebugSession session) throws IOException, ExecutionException {
    super(session);
    session.setPauseActionSupported(true);
    //session.setCurrentStackFrame();
    final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), runner);
    myProcessHandler = executionResult.getProcessHandler();
    myProcessHandler.putUserData(KEY, this);
    myExecutionConsole = executionResult.getExecutionConsole();
    myEditorsProvider = new Native2DebuggerEditorsProvider();
    Disposer.register(myExecutionConsole, this);
    @Nullable OutputStream childIn = executionResult.getProcessHandler().getProcessInput();
    myChildIn = childIn;
    send("-file-exec-and-symbols", "/home/dannym/src/Oxide/main/amd-host-image-builder/target/debug/amd-host-image-builder");
    myDebuggerSession = new Native2DebuggerSession(this);

    // too early. session.initBreakpoints();
    // after initBreakpoints; send("-exec-run");
//      final List<Breakpoint> breakpoints = myBreakpointManager.getBreakpoints();

    // TODO: -file-list-exec-source-files, -file-list-shared-libraries, -file-list-symbol-files,
  }

  public void handleGdbMiLine(String line) {
    if (line.startsWith("*") || line.startsWith("=") || line.startsWith("^")) { // async, async, sync
      System.err.println("QUARTER WRITTEN " + line);
      if (line.startsWith("*stopped,")) {
        String[] parts = line.split(",");
        String xfile = null;
        String xline = null;
        for (String part : parts) {
          String[] kvs = part.split("=");
          if (kvs.length == 2) {
            String k = kvs[0];
            String v = kvs[1];
            if (v.startsWith("\"")) {
              v = v.substring(1, v.length() - 1);
            }

            if (k.equals("line")) {
              xline = v;
            } else if (k.equals("file")) { // or "fullname"
              xfile = v;
            }

          }
        }
        if (xfile != null && xline != null) {
          // FIXME getSession().positionReached(new MySuspendContext(myDebuggerSession, c.getCurrentFrame(), c.getSourceFrame()));
          System.err.println("xfile: " + xfile);
          System.err.println("xline: " + xline);
        }
      }
    }
  }

  // We'll call initBreakpoints() at the right time on our own.
  @Override
  public boolean checkCanInitBreakpoints() {
    // FIXME: That is a hack
    ApplicationManager.getApplication().invokeLater(() -> {
      //getSession().initBreakpoints();
      System.err.println("EXEC RUN");
      send("-exec-run");
    });

    return true;
  }
  @Override
  public XBreakpointHandler<?> /*@NotNull*/ [] getBreakpointHandlers() {
    return myXBreakpointHandlers;
  }

  public BreakpointManager getBreakpointManager() {
    return myBreakpointManager;
  }

  @Nullable
  public static Native2DebugProcess getInstance(ProcessHandler handler) {
    return handler.getUserData(KEY);
  }

  @NotNull
  @Override
  public ExecutionConsole createConsole() {
    return myExecutionConsole;
  }

  @Override
  protected ProcessHandler doGetProcessHandler() {
    return myProcessHandler;
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return myEditorsProvider;
  }

  @Override
  public void startStepOver(@Nullable XSuspendContext context) {
    send("-exec-next");
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    send("-exec-step");
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    send("-exec-finish");
  }

  @Override
  public void startPausing() {
    send("-exec-interrupt");
    //getSession().pause();
  }
  @Override
  public void stop() {
    // Note: IDEA usually calls this AFTER the process was already terminated.
    if (!myProcessHandler.isProcessTerminated()) {
      send("-gdb-exit");
    }
  }

  @Override
  public boolean checkCanPerformCommands() {
    if (myDebuggerSession == null)
        return super.checkCanPerformCommands();
    return true;
  }

  @Override
  public void resume(@Nullable XSuspendContext context) {
    send("-exec-continue");
  }

  @Override
  public void dispose() {
  }

  @Override
  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    try {
      send("-exec-until", fileLineReference(position));
    } catch (RuntimeException e) {
      e.printStackTrace();
      final PsiFile psiFile = PsiManager.getInstance(getSession().getProject()).findFile(position.getFile());
      assert psiFile != null;
      StatusBar.Info.set(Native2DebuggerBundle.message("status.bar.text.not.valid.position.in.file", psiFile.getName()), psiFile.getProject());
      //final Debugger c = myDebuggerSession.getClient();
      // TODO: Context: Stack Frames, Variable Table, Evaluated Expressions
      // FIXME getSession().positionReached(new MySuspendContext(myDebuggerSession, c.getCurrentFrame(), c.getSourceFrame()));
    }
  }

  private static class MySuspendContext extends XSuspendContext {
    private final Native2DebuggerSession myDebuggerSession;
    private final Debugger.StyleFrame myStyleFrame;
    private final Debugger.SourceFrame mySourceFrame;

    MySuspendContext(Native2DebuggerSession debuggerSession, Debugger.StyleFrame styleFrame, Debugger.SourceFrame sourceFrame) {
      myDebuggerSession = debuggerSession;
      myStyleFrame = styleFrame;
      mySourceFrame = sourceFrame;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
      return new Native2ExecutionStack(Native2DebuggerBundle.message("list.item.native2.frames"), myStyleFrame, myDebuggerSession);
    }

    public XExecutionStack getSourceStack() {
      return new Native2ExecutionStack(Native2DebuggerBundle.message("list.item.source.frames"), mySourceFrame, myDebuggerSession);
    }

    @Override
    public XExecutionStack /*@NotNull*/ [] getExecutionStacks() {
      return new XExecutionStack[]{
        getActiveExecutionStack(),
        getSourceStack()
      };
    }
  }
}
