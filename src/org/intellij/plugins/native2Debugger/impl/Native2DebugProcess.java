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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static javax.swing.text.html.HTML.Attribute.MULTIPLE;

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

  public void handleGdbMiStateOutput(char mode, String klass, HashMap<String, Object> attributes) {
     // =breakpoint-modified{bkpt={number=1, times=0, original-location=/home/dannym/src/Oxide/main/amd-host-image-builder/src/main.rs:2472, locations=[{number=1.1, thread-groups=[i1], file=src/main.rs, func=amd_host_image_builder::main, line=2472, fullname=/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs, addr=0x00007ffff7b538d4, enabled=y}, {number=1.2, thread-groups=[i1], file=src/main.rs, func=amd_host_image_builder::main, line=2472, fullname=/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs, addr=0x00007ffff7b53a70, enabled=y}], type=breakpoint, addr=<MULTIPLE>, disp=keep, enabled=y}}

    if (mode == '=' && klass.equals("breakpoint-modified") && attributes.containsKey("bkpt")) {
      try {
        HashMap<String, Object> bkpt = (HashMap<String, Object>) attributes.get("bkpt");
        String number = (String) bkpt.get("number");
        String times = (String) bkpt.get("times");
        String originalLocation = (String) bkpt.get("original-location");
        String type_ = (String) bkpt.get("breakpoint");
        // String addr
        String disp = (String) bkpt.get("disp");
        String enabled = (String) bkpt.get("enabled");
        ArrayList<Object> locations = (ArrayList<Object>) bkpt.get("locations");
        System.err.println("breakpoint " + originalLocation + " " + enabled);
      } catch (ClassCastException e) {
        System.err.println("handleGdbMiStateOutput failed... " + attributes);
        e.printStackTrace();
      }
    } else if (mode == '*' && klass.equals("stopped")) {
      //*stopped,reason="breakpoint-hit",disp="keep",bkptno="1",frame={addr="0x00007ffff7b53857",func="amd_host_image_builder::main",args=[],file="src/main.rs",fullname="/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs",line="2469",arch="i386:x86-64"},thread-id="1",stopped-threads="all",core="4"
      // FIXME: The point here is to change the IDEA debugger state to paused or something
      try {
//        String reason = (String) attributes.get("reason");
//        String disp = (String) attributes.get("disp");
//        String bkptno = (String) attributes.get("bkptno");
//        String threadId = (String) attributes.get("thread-id");
//        String stoppedThreads = (String) attributes.get("stopped-threads");
//        String core = (String) attributes.get("core");
        if (attributes.containsKey("frame")) {
          HashMap<String, Object> frame = (HashMap<String, Object>) attributes.get("frame");
          getSession().positionReached(new MySuspendContext(this.myDebuggerSession, frame));
          String addr = (String) frame.get("addr");
          String func = (String) frame.get("func");
          String args = (String) frame.get("args");
          String file = (String) frame.get("file");
          String fullname = (String) frame.get("fullname");
          String line = (String) frame.get("line");
          String arch = (String) frame.get("arch");
        }
      } catch (ClassCastException e) {
        System.err.println("handleGdbMiStateOutput failed... " + attributes);
        e.printStackTrace();
      }
    }
  }

  void send(String operation, String[] options, String[] parameters) {
    try {
      myChildIn.write(operation.getBytes(StandardCharsets.UTF_8));
      for (String option: options) {
        myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
        myChildIn.write(option.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
      }
      if (parameters.length > 0) {
        myChildIn.write(" --".getBytes(StandardCharsets.UTF_8));
        for (String parameter: parameters) {
          myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
          myChildIn.write(parameter.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
        }
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
    send("-file-exec-and-symbols", new String[] {"/home/dannym/src/Oxide/main/amd-host-image-builder/target/debug/amd-host-image-builder"}, new String[0]);
    myDebuggerSession = new Native2DebuggerSession(this);

    // too early. session.initBreakpoints();
    // after initBreakpoints; send("-exec-run");
//      final List<Breakpoint> breakpoints = myBreakpointManager.getBreakpoints();

    // TODO: -file-list-exec-source-files, -file-list-shared-libraries, -file-list-symbol-files,
  }

  // We'll call initBreakpoints() at the right time on our own.
  @Override
  public boolean checkCanInitBreakpoints() {
    // FIXME: That is a hack
    ApplicationManager.getApplication().invokeLater(() -> {
      //getSession().initBreakpoints();
      System.err.println("EXEC RUN");
      send("-exec-run", new String[0], new String[0]);
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
    send("-exec-next", new String[0], new String[0]);
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    send("-exec-step", new String[0], new String[0]);
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    send("-exec-finish", new String[0], new String[0]);
  }

  @Override
  public void startPausing() {
    send("-exec-interrupt", new String[0], new String[0]);
    //getSession().pause();
  }
  @Override
  public void stop() {
    // Note: IDEA usually calls this AFTER the process was already terminated.
    if (!myProcessHandler.isProcessTerminated()) {
      send("-gdb-exit", new String[0], new String[0]);
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
    send("-exec-continue", new String[0], new String[0]);
  }

  @Override
  public void dispose() {
  }

  @Override
  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    try {
      send("-exec-until", new String[] { fileLineReference(position) }, new String[0]);
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
    private final HashMap<String, Object> myGdbExecutionFrame;

    MySuspendContext(Native2DebuggerSession debuggerSession, HashMap<String, Object> gdbExecutionFrame) {
      myDebuggerSession = debuggerSession;
      myGdbExecutionFrame = gdbExecutionFrame;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
      return new Native2ExecutionStack(Native2DebuggerBundle.message("list.item.native2.frames"), myGdbExecutionFrame, myDebuggerSession);
    }

    public XExecutionStack getSourceStack() {
      return new Native2ExecutionStack(Native2DebuggerBundle.message("list.item.source.frames"), myGdbExecutionFrame, myDebuggerSession); // FIXME
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
