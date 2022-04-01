// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.friendly_machines.intellij.plugins.native2Debugger.*;
import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.Breakpoint;
import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.BreakpointManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.pty4j.unix.Pty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.*;

// TODO:  -break-condition, -break-list, -break-delete, -break-disable, -break-enable, -break-passcount, -break-watch, -catch-load
// TODO: -environment-cd, -environment-directory, -environment-pwd
// TODO: -thread-info, -thread-list-ids, -thread-select
// TODO: -stack-info-frame
// TODO: fixed variable object, floating variable object, -var-create, -var-delete, -var-info-type, -var-info-expression, -var-info-path-expression, -var-show-attributes, -var-evaluate-expression, -var-assign, -var-update, -var-set-frozen, -var-set-update-range
// TODO: -data-read-memory-bytes, -data-write-memory-bytes
// TODO: tracepoints, -trace-find, -trace-define-variable, -trace-frame-collected, -trace-list-variables, -trace-start, -trace-save
// TODO: registerAdditionalActions(DefaultActionGroup leftToolbar, DefaultActionGroup topToolbar, DefaultActionGroup settings) ?
// TODO: public XValueMarkerProvider<?,?> createValueMarkerProvider(); If debugger values have unique ids just return these ids from getMarker(XValue) method. Alternatively implement markValue(XValue) to store a value in some registry and implement unmarkValue(XValue, Object) to remote it from the registry. In such a case the getMarker(XValue) method can return null if the value isn't marked.

// TODO: exec-jump fileline
// TODO: -exec-next-instruction
// ?: -symbol-info-functions, -symbol-info-module-functions, -symbol-info-module-variables, -symbol-info-modules, -symbol-info-types, -symbol-info-variables, -symbol-list-lines

// See <https://dploeger.github.io/intellij-api-doc/com/intellij/xdebugger/XDebugProcess.html>
public class DebugProcess extends XDebugProcess implements Disposable {
    private static final Key<DebugProcess> KEY = Key.create("PROCESS");
    public static final Key<GdbMiFilter> MI_FILTER = Key.create("MI_FILTER");

    private final EditorsProvider myEditorsProvider;
    private final ProcessHandler myProcessHandler;
    private final ExecutionConsole myExecutionConsole;
    //private final OutputStream myChildIn;

    private BreakpointManager myBreakpointManager = new BreakpointManager(this);

    private final XBreakpointHandler<?>[] myXBreakpointHandlers = new XBreakpointHandler<?>[]{
            new BreakpointHandler(this, BreakpointType.class),
    };

    private GdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        GdbMiFilter filter = myProcessHandler.getUserData(MI_FILTER);
        return filter.gdbSend(operation, options, parameters);
    }

    private HashMap<String, Object> gdbCall(String operation, String[] options, String[] parameters) throws GdbMiOperationException {
        GdbMiFilter filter = myProcessHandler.getUserData(MI_FILTER);
        return filter.gdbCall(operation, options, parameters);
    }

    private void handleGdbMiNotifyAsyncOutput(String klass, HashMap<String, Object> attributes) {
        if ((klass.equals("breakpoint-modified") || klass.equals("breakpoint-created") || klass.equals("breakpoint-deleted")) && attributes.containsKey("bkpt")) {
            // Note: if a breakpoint is emitted in the result record of a command, then it will not also be emitted in an async record.
            try {
                HashMap<String, Object> bkpt = (HashMap<String, Object>) attributes.get("bkpt");
                String number = (String) bkpt.get("number");
                if (klass.equals("breakpoint-deleted")) {
                    myBreakpointManager.deleteBreakpoint1(number);
                } else {
                    Optional<Breakpoint> breakpointo = myBreakpointManager.getBreakpointByGdbNumber(number);
                    if (breakpointo.isPresent()) {
                        Breakpoint breakpoint = breakpointo.get();
                        breakpoint.setFromGdbBkpt(bkpt);
                    }
                }
            } catch (ClassCastException e) {
                reportError("handleGdbMiNotifyAsyncOutput failed with: " + attributes);
                e.printStackTrace();
            }
        } else {
            // TODO: thread-group-added (id), thread-group-removed (id), thread-group-started (id, pid), thread-group-exited (id, exit-code), thread-created (id, group-id), thread-exited (id, group-id), thread-selected (id, frame), "library-loaded"
            getSession().reportMessage(klass + " " + attributes.toString(), MessageType.INFO);
        }
    }

    private void handleGdbMiExecAsyncOutput(String klass, HashMap<String, Object> attributes) {
        if (klass.equals("stopped")) {
            // TODO: running with thread-id (or "all"), stopped with thread-id or stopped (a list of ids or "all")
            //*stopped,reason="breakpoint-hit",disp="keep",bkptno="1",frame={addr="0x00007ffff7b53857",func="amd_host_image_builder::main",args=[],file="src/main.rs",fullname="/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs",line="2469",arch="i386:x86-64"},thread-id="1",stopped-threads="all",core="4"
            // Note: The point here is to change the IDEA debugger state to paused
            try {
                String reason = (String) attributes.get("reason");
//        String disp = (String) attributes.get("disp");
//        String bkptno = (String) attributes.get("bkptno");
//        String threadId = (String) attributes.get("thread-id");
//        String stoppedThreads = (String) attributes.get("stopped-threads");
//        String core = (String) attributes.get("core");

                HashMap<String, Object> tresponse = getThreadInfo();
                if (tresponse.containsKey("threads")) { // response from -thread-info; FIXME: make that better.
                    List<Object> threads = (List<Object>) tresponse.get("threads");
                    String currentThreadId = (String) tresponse.get("current-thread-id");

                    SuspendContext context = generateSuspendContext(threads, currentThreadId);
                    if ("breakpoint-hit".equals(reason)) {
                        if (attributes.containsKey("bkptno")) {
                            String bkptno = (String) attributes.get("bkptno");
                            Optional<Breakpoint> breakpointo = myBreakpointManager.getBreakpointByGdbNumber(bkptno);
                            if (breakpointo.isPresent()) {
                                Breakpoint breakpoint = breakpointo.get();
                                getSession().breakpointReached(breakpoint.getXBreakpoint(), "fancy message", context);
                            }
                        } else {
                            reportError("Unknown GDB breakpoint was hit");
                        }
                    }
                    getSession().positionReached(context); // TODO: Only for "Run to Cursor" ?
                } else {
                    reportError("handleGdbMiExecAsyncOutput failed with: no threads response in " + attributes);
                }
            } catch (ClassCastException e) {
                e.printStackTrace();
                reportError("handleGdbMiExecAsyncOutput failed with: " + attributes);
            } catch (GdbMiOperationException e) {
                reportError("handleGdbMiExecAsyncOutput failed", e);
            }
        }
    }

    public void handleGdbMiStateOutput(GdbMiStateResponse response) {
        // =breakpoint-modified{bkpt={number=1, times=0, original-location=/home/dannym/src/Oxide/main/amd-host-image-builder/src/main.rs:2472, locations=[{number=1.1, thread-groups=[i1], file=src/main.rs, func=amd_host_image_builder::main, line=2472, fullname=/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs, addr=0x00007ffff7b538d4, enabled=y}, {number=1.2, thread-groups=[i1], file=src/main.rs, func=amd_host_image_builder::main, line=2472, fullname=/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs, addr=0x00007ffff7b53a70, enabled=y}], type=breakpoint, addr=<MULTIPLE>, disp=keep, enabled=y}}
        char mode = response.getMode();
        String klass = response.getKlass();
        HashMap<String, Object> attributes = response.getAttributes();
        if (mode == '=') {
            handleGdbMiNotifyAsyncOutput(klass, attributes);
        } else if (mode == '*') {
            handleGdbMiExecAsyncOutput(klass, attributes);
        }
    }

    public void reportError(String s) {
        getSession().reportError(s);
    }

    public void reportError(String s, GdbMiOperationException e) {
        GdbMiStateResponse details = e.getDetails();
        if (details != null) {
            HashMap<String, Object> attributes = details.getAttributes();
            if (attributes != null) {
                Object msg = attributes.get("msg");
                if (msg != null) {
                    reportError(s + ": " + msg);
                    return;
                }
            }
        }
        reportError(s + ":" + e.toString());
    }

    private SuspendContext generateSuspendContext(List<Object> threads, String currentThreadId) {
        ArrayList<ExecutionStack> stacks = new ArrayList<>();
        int activeStackId = -1;

        for (Object thread1: threads) {
            HashMap<String, Object> thread = (HashMap<String, Object>) thread1;
            String id = (String) thread.get("id");
            String name = thread.containsKey("target-id") ? (String) thread.get("target-id") : id;
            String state = thread.containsKey("state") ? (String) thread.get("state") : "";
            if (state.length() > 0) {
                name = name + ": " + state;
            }
            if (thread.containsKey("details")) {
                name = name + "; " + (String) thread.get("details");
            }
            Optional<HashMap<String, Object>> topFrame = thread.containsKey("frame") ? Optional.of((HashMap<String, Object>) thread.get("frame")) : Optional.empty();
            //Native2ExecutionStack(@NlsContexts.ListItem String name, List<Map.Entry<String, Object>> frames, Native2DebugProcess debuggerSession) {
            ExecutionStack stack = new ExecutionStack(name, id, topFrame, this); // one per thread
            stacks.add(stack);
            if (currentThreadId.equals(id)) {
                activeStackId = stacks.size() - 1;
            }
        }
        SuspendContext context = new SuspendContext(this, stacks.toArray(new ExecutionStack[0]), activeStackId);
        return context;
    }

    public List<HashMap<String, Object>> getVariables(String threadId, String frameId) throws GdbMiOperationException {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        // TODO: --simple-values and find stuff yourself.
        HashMap<String, Object> q = gdbCall("-stack-list-variables", new String[] { "--thread", threadId, "--frame", frameId, "--all-values" }, new String[] {  });
        if (q.containsKey("variables")) {
            try {
                List<? extends Object> variables = (List<? extends Object>) q.get("variables");
                for (Object variable1 : variables) {
                    HashMap<String, Object> variable = (HashMap<String, Object>) variable1;
                    result.add(variable);
                }
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public List<HashMap<String, Object>> getFrames(String threadId) throws GdbMiOperationException {
        List<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> q = gdbCall("-stack-list-frames", new String[]{"--thread", threadId}, new String[0]);
        if (q.containsKey("stack")) {
            try {
                List<? extends Object> stack = (List<? extends Object>) q.get("stack");
                for (Object frame1 : stack) {
                    Map.Entry<String, Object> frame = (Map.Entry<String, Object>) frame1;
                    if ("frame".equals(frame.getKey())) {
                        result.add((HashMap<String, Object>) frame.getValue());
                    }
                }
            } catch (ClassCastException e) {
                // Note: This can be because it was a [] that was interpreted as List<String>
                e.printStackTrace();
            }
        } else {
            reportError("could not get stack frames of thread");
        }
        return result;
    }

    private HashMap<String, Object> getThreadInfo() throws GdbMiOperationException {
        return gdbCall("-thread-info", new String[] {}, new String[0]);
    }

    private void loadSymbols(String filename) throws GdbMiOperationException {
        gdbCall("-file-symbol-file", new String[] { filename }, new String[0]);
    }

    private static boolean isFileExecutable(VirtualFile file) {
        @Nullable String extension = file.getExtension();
        if (file.isDirectory()) {
            return "app".equals(extension); // MacOS X
        }
        if ("exe".equals(extension)) { // Windows
            return true;
        }
//        if (extension == null) { // UNIX
//            // ./platform/lang-impl/src/com/intellij/openapi/fileTypes/impl/associate/OSAssociateFileTypesUtil.java
//            return true;
//        }
        if (file.isInLocalFileSystem()) {
            File f = new File(file.getPath());
            if (f.canExecute()) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }
    private void loadExecutable(ExecutionEnvironment environment, String configuredExecutableName) {
        if (configuredExecutableName == null || configuredExecutableName.isEmpty()) {
            @Nullable VirtualFile preselectedExecutable = null;
            // guessProjectDir is not perfect. A project has modules. Modules have content roots. Content roots can be anywhere. The module configuration file (iml) can be anywhere.
            @Nullable String path = environment.getModulePath(); // often null
            @Nullable VirtualFile base = path != null ? LocalFileSystem.getInstance().findFileByPath(path) : null;
            if (base == null) {
                base = ProjectUtil.guessProjectDir(environment.getProject());
                path = base.getPath();
            }
            if (base != null) {
                if (base.findFileByRelativePath("target") != null) { // Rust
                    base = base.findFileByRelativePath("target");
                    path = base.getPath();
                }
                // see ./platform/lang-impl/src/com/intellij/find/impl/
                List<VirtualFile> result = VfsUtil.collectChildrenRecursively(base);
                int count = 0;
                for (VirtualFile virtualFile : result) {
                    if (isFileExecutable(virtualFile)) {
                        preselectedExecutable = virtualFile;
                        //System.err.println("EXEC " + virtualFile.getPath());
                        ++count;
                    }
                }
//                        if (count > 1) {
//                            preselectedExecutable = null;
//                        }
            }

            // FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor();
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                @Override
                public boolean isFileSelectable(@Nullable VirtualFile file) {
                    return isFileExecutable(file);
                }
            };
            Component parentComponent =  null; // TODO
            FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, environment.getProject(), parentComponent);
            // TODO: Make it open a useful directory (for example PATH)
            VirtualFile[] selectedExecutables = chooser.choose(environment.getProject(), new VirtualFile[]{preselectedExecutable});
            //FileChooser.chooseFile(, environment.getProject(), preselectedExecutable);

            VirtualFile selectedExecutable = selectedExecutables.length > 0 ? selectedExecutables[0] : null;
            if (selectedExecutable != null) {
                configuredExecutableName = selectedExecutable.getPath();
            }
        }
        try {
            if (configuredExecutableName != null && !configuredExecutableName.isEmpty()) {
                gdbTarget("exec", configuredExecutableName);

                try {
                    loadSymbols(configuredExecutableName);
                } catch (GdbMiOperationException e) {
                    reportError("Loading symbols failed", e);
                }
            } else {
                gdbTarget("exec"); // not that useful, but ehh.
            }
        } catch (GdbMiOperationException e) {
            reportError("Could not load executable", e);
        }
    }
    public DebugProcess(RunProfileState runProfileState, ExecutionEnvironment environment, Runner runner, XDebugSession session) throws IOException, ExecutionException {
        super(session);
        session.setPauseActionSupported(true);
        //session.setCurrentStackFrame();
        final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), runner);
        myProcessHandler = executionResult.getProcessHandler();
        myProcessHandler.putUserData(KEY, this);
        Pty myPty = myProcessHandler.getUserData(RunProfileState.PTY);
        myExecutionConsole = executionResult.getExecutionConsole();
        myEditorsProvider = new EditorsProvider();
        GdbMiFilter filter = new GdbMiFilter(this, environment.getProject(), myPty, myPty.getOutputStream());
        myProcessHandler.putUserData(MI_FILTER, filter);
        myProcessHandler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent processEvent) {
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent processEvent) {
                try {
                    // TODO: Read everything from myPtr (for MacOS)
                    myPty.close();
                    myProcessHandler.putUserData(MI_FILTER, null);
                    myProcessHandler.putUserData(RunProfileState.PTY, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {

            }
        });

        Disposer.register(myExecutionConsole, this);
        @Nullable OutputStream childIn = executionResult.getProcessHandler().getProcessInput();
        //myChildIn = childIn;
        try {
            gdbSet("mi-async", "on");
        } catch (GdbMiOperationException e) {
            reportError("mi-async on failed", e);
        }
        //gdbSet("interactive-mode", "on"); // just in case we use a pipe for communicating with gdb: force pty-like communication
        gdbSend("-enable-frame-filters", new String[] {}, new String[0]);
        ProjectSettingsState projectSettings = ProjectSettingsState.getInstance();
        try {
            gdbSet("sysroot", projectSettings.gdbSysRoot);
        } catch (GdbMiOperationException e) {
            // TODO: Maybe show dialog box
            StatusBar.Info.set("Could not set sysroot to " + projectSettings.gdbSysRoot, environment.getProject());
        }
        try {
            gdbSet("arch", projectSettings.gdbArch);
        } catch (GdbMiOperationException e) {
            // TODO: Maybe show dialog box
            StatusBar.Info.set("Could not set arch to " + projectSettings.gdbArch, environment.getProject());
        }
        if ("exec".equals(projectSettings.gdbTargetType)) {
            loadExecutable(environment, projectSettings.gdbTargetArg);
        } else try {
            if (projectSettings.symbolFile != null && !projectSettings.symbolFile.isEmpty()) {
                loadSymbols(projectSettings.symbolFile);
            } else {
                if ("exec".equals(projectSettings.gdbTargetType)) {
                    loadSymbols(projectSettings.gdbTargetArg);
                }
            }
        } catch (GdbMiOperationException e) {
            reportError("Loading symbols failed", e);
        }

//        gdbSend("-file-exec-and-symbols", new String[]{"/home/dannym/src/Oxide/main/amd-host-image-builder/target/debug/amd-host-image-builder"}, new String[0]);
        // TODO: -exec-arguments args
        //myDebuggerSession = new Native2DebuggerSession(this);

        // too early. session.initBreakpoints();
        // after initBreakpoints; send("-exec-run");
//      final List<Breakpoint> breakpoints = myBreakpointManager.getBreakpoints();

        // TODO: -file-list-exec-source-files, -file-list-shared-libraries, -file-list-symbol-files,
    }

    private void gdbTarget(String gdbTargetType, String gdbTargetArg) throws GdbMiOperationException {
        gdbCall("-target-select", new String[] { gdbTargetType, gdbTargetArg }, new String[] {});
    }

    private void gdbTarget(String gdbTargetType) throws GdbMiOperationException {
        gdbCall("-target-select", new String[] { gdbTargetType }, new String[] {});
    }

    private void gdbSet(String key, String value) throws GdbMiOperationException {
        gdbCall("-gdb-set", new String[] { key, value }, new String[] {});
    }

    private void execRun() throws GdbMiOperationException {
        System.err.println("EXEC RUN");
        gdbCall("-exec-run", new String[0], new String[0]);
    }

    // We'll call initBreakpoints() at the right time on our own.
    @Override
    public boolean checkCanInitBreakpoints() {
        // FIXME: That is a hack
        ApplicationManager.getApplication().invokeLater(() -> {
            //getSession().initBreakpoints();
            try {
                execRun();
            } catch (GdbMiOperationException e) {
                reportError("exec-run failed", e);
            }
        });

        return true;
    }

    @Override
    public XBreakpointHandler<?> /*@NotNull*/[] getBreakpointHandlers() {
        return myXBreakpointHandlers;
    }

    public BreakpointManager getBreakpointManager() {
        return myBreakpointManager;
    }

    @Nullable
    public static DebugProcess getInstance(ProcessHandler handler) {
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
        gdbSend("-exec-next", new String[0], new String[0]);
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        gdbSend("-exec-step", new String[0], new String[0]);
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        gdbSend("-exec-finish", new String[0], new String[0]);
    }

    @Override
    public void startPausing() {
        gdbSend("-exec-interrupt", new String[0], new String[0]);
        //getSession().pause();
    }

    @Override
    public void stop() {
        // Note: IDEA usually calls this AFTER the process was already terminated.
        if (!myProcessHandler.isProcessTerminated()) {
            gdbSend("-gdb-exit", new String[0], new String[0]);
        }
    }

    @Override
    public boolean checkCanPerformCommands() {
        //if (myDebuggerSession == null)
//            return super.checkCanPerformCommands();
        return true;
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        gdbSend("-exec-continue", new String[0], new String[0]);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
        try {
            gdbSend("-exec-until", new String[]{BreakpointManager.fileLineReference(position)}, new String[0]);
        } catch (RuntimeException e) {
            e.printStackTrace();
            final PsiFile psiFile = PsiManager.getInstance(getSession().getProject()).findFile(position.getFile());
            assert psiFile != null;
            StatusBar.Info.set(DebuggerBundle.message("status.bar.text.not.valid.position.in.file", psiFile.getName()), psiFile.getProject());
            //final Debugger c = myDebuggerSession.getClient();
            // TODO: Context: Stack Frames, Variable Table, Evaluated Expressions
            // FIXME getSession().positionReached(new MySuspendContext(myDebuggerSession, c.getCurrentFrame(), c.getSourceFrame()));
        }
    }

    public HashMap<String, Object> dprintfInsert(String[] options, String[] parameters) throws GdbMiOperationException {
        return gdbCall("-dprintf-insert", options, parameters);
    }

    public HashMap<String, Object> breakInsert(String[] options, String[] parameters) throws GdbMiOperationException {
        return gdbCall("-break-insert", options, parameters);
    }

    public void breakDelete(String number) throws GdbMiOperationException {
        gdbCall("-break-delete", new String[] { number }, new String[] { });
    }

    public void breakEnable(String number) throws GdbMiOperationException {
        gdbCall("-break-enable", new String[] { number }, new String[] {  });
    }
    public void breakDisable(String number) throws GdbMiOperationException {
        gdbCall("-break-disable", new String[] { number }, new String[] { });
    }

    public HashMap<String, Object> evaluate(String expr, String threadId, String frameId) throws GdbMiOperationException {
        return gdbCall("-data-evaluate-expression", new String[] { "--thread", threadId, "--frame", frameId,  expr }, new String[0]);
    }
}
