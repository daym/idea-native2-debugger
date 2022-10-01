// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.friendly_machines.intellij.plugins.ideanative2debugger.*;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
    public static final Key<DebugProcess> DEBUG_PROCESS_KEY = Key.create("DEBUG_PROCESS"); // FIXME

    private final EditorsProvider myEditorsProvider;
    private final ProcessHandler myProcessHandler;
    private final ExecutionConsole myExecutionConsole;
    private final GdbMiFilter myMiFilter;
    private final ExecutionEnvironment myEnvironment;
    //private final OutputStream myChildIn;

    protected volatile boolean isGDBconnected = false;

    private BreakpointManager myBreakpointManager = new BreakpointManager(this);

    private final XBreakpointHandler<?>[] myXBreakpointHandlers = new XBreakpointHandler<?>[]{
            new BreakpointHandler(this, BreakpointType.class),
    };

    private GdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        return myMiFilter.gdbSend(operation, options, parameters);
    }

    private Map<String, Object> gdbCall(String operation, String[] options, String[] parameters) throws GdbMiOperationException {
        return myMiFilter.gdbCall(operation, options, parameters);
    }

    private void handleGdbMiNotifyAsyncOutput(String klass, HashMap<String, Object> attributes) {
        if ((klass.equals("breakpoint-modified") || klass.equals("breakpoint-created") || klass.equals("breakpoint-deleted")) && attributes.containsKey("bkpt")) {
            // Note: if a breakpoint is emitted in the result record of a command, then it will not also be emitted in an async record.
            try {
                HashMap<String, Object> bkpt = (HashMap<String, Object>) attributes.get("bkpt");
                String number = (String) bkpt.get("number");
                if (klass.equals("breakpoint-deleted")) {
                    myBreakpointManager.deleteBreakpointByGdbNumber(number);
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
            // *stopped,reason="breakpoint-hit",disp="keep",bkptno="1",frame={addr="0x00007ffff7b53857",func="amd_host_image_builder::main",args=[],file="src/main.rs",fullname="/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs",line="2469",arch="i386:x86-64"},thread-id="1",stopped-threads="all",core="4"
            // Note: The point here is to change the IDEA debugger state to paused
            try {
                String reason = (String) attributes.get("reason");
//        String disp = (String) attributes.get("disp");
//        String bkptno = (String) attributes.get("bkptno");
//        String threadId = (String) attributes.get("thread-id");
//        String stoppedThreads = (String) attributes.get("stopped-threads");
//        String core = (String) attributes.get("core");

                var tresponse = getThreadInfo();
                if (tresponse.containsKey("threads")) {
                    List<Object> threads = (List<Object>) tresponse.get("threads");
                    String currentThreadId = (String) tresponse.get("current-thread-id");

                    SuspendContext context = generateSuspendContext(threads, currentThreadId);
                    if ("breakpoint-hit".equals(reason)) {
                        if (attributes.containsKey("bkptno")) {
                            String bkptno = (String) attributes.get("bkptno");
                            Optional<Breakpoint> breakpointo = myBreakpointManager.getBreakpointByGdbNumber(bkptno);
                            if (breakpointo.isPresent()) {
                                Breakpoint breakpoint = breakpointo.get();
                                getSession().breakpointReached(breakpoint.getXBreakpoint(), "fancy message", context); // FIXME
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

    public void handleGdbTextOutput(char mode, @NotNull String text) {
        switch (mode) {
            case '&': // log
                if (!text.isEmpty())
                    reportMessage(text, MessageType.INFO);
                break;
            case '@': // target
            {
//                var view = (ConsoleView) myExecutionConsole.getComponent();
//                if (!text.isEmpty())
//                    view.print(text, ConsoleViewContentType.SYSTEM_OUTPUT);
                if (!text.isEmpty()) {
                    reportMessage(text, MessageType.INFO);
                }
                break;
            }
            case '~': // console
                if (!text.isEmpty())
                    reportMessage(text, MessageType.INFO);
                break;
            default:
                break;
        }
    }


    public void reportError(String s) {
        getSession().reportError(s);
    }

    public void reportMessage(@NotNull @NlsContexts.NotificationContent String text, @NotNull MessageType typ) {
        getSession().reportMessage(text, typ);
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

    private static String getThreadName(HashMap<String, Object> thread, String id) {
        String name = thread.containsKey("target-id") ? (String) thread.get("target-id") : id;
        String state = thread.containsKey("state") ? (String) thread.get("state") : "";
        if (state.length() > 0) {
            name = name + ": " + state;
        }
        if (thread.containsKey("details")) {
            name = name + "; " + (String) thread.get("details");
        }
        return name;
    }

    private SuspendContext generateSuspendContext(List<Object> threads, String currentThreadId) {
        ArrayList<ExecutionStack> stacks = new ArrayList<>();
        int activeStackId = -1;

        for (Object thread1 : threads) {
            HashMap<String, Object> thread = (HashMap<String, Object>) thread1;
            String id = (String) thread.get("id");
            String name = getThreadName(thread, id);
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
        var q = gdbCall("-stack-list-variables", new String[]{"--thread", threadId, "--frame", frameId, "--all-values"}, new String[]{});
        if (q.containsKey("variables")) {
            try {
                List<?> variables = (List<?>) q.get("variables");
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
        var q = gdbCall("-stack-list-frames", new String[]{"--thread", threadId}, new String[0]);
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

    private Map<String, Object> getThreadInfo() throws GdbMiOperationException {
        return gdbCall("-thread-info", new String[]{}, new String[0]);
    }

    private void loadSymbols(String filename) throws GdbMiOperationException {
        gdbCall("-file-symbol-file", new String[]{filename}, new String[0]);
    }

    private void gdbTarget(String gdbTargetType, String gdbTargetArg) throws GdbMiOperationException {
        gdbCall("-target-select", new String[]{gdbTargetType, gdbTargetArg}, new String[]{});
    }

    private void gdbTarget(String gdbTargetType) throws GdbMiOperationException {
        gdbCall("-target-select", new String[]{gdbTargetType}, new String[]{});
    }

    private void gdbSet(String key, String value) throws GdbMiOperationException {
        gdbCall("-gdb-set", new String[]{key, value}, new String[]{});
    }

    public Map<String, Object> dprintfInsert(String[] options, String[] parameters) throws GdbMiOperationException {
        return gdbCall("-dprintf-insert", options, parameters);
    }

    public Map<String, Object> breakInsert(String[] options, String[] parameters) throws GdbMiOperationException {
        return gdbCall("-break-insert", options, parameters);
    }

    public void breakDelete(String number) throws GdbMiOperationException {
        gdbCall("-break-delete", new String[]{number}, new String[]{});
    }

    public void breakEnable(String number) throws GdbMiOperationException {
        gdbCall("-break-enable", new String[]{number}, new String[]{});
    }

    public void breakDisable(String number) throws GdbMiOperationException {
        gdbCall("-break-disable", new String[]{number}, new String[]{});
    }

    public Map<String, Object> evaluate(String expr, String threadId, String frameId) throws GdbMiOperationException {
        return gdbCall("-data-evaluate-expression", new String[]{"--thread", threadId, "--frame", frameId, expr}, new String[0]);
    }

    private void execRun() throws GdbMiOperationException {
        System.err.println("EXEC RUN");
        gdbCall("-exec-run", new String[0], new String[0]);
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
            return true; // TODO
        }
        return false;
    }

    @Nullable
    private VirtualFile getPreselectedExecutable(ExecutionEnvironment environment, @Nullable String path) {
        @Nullable VirtualFile preselectedExecutable = null;
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
        return preselectedExecutable;
    }

    @Nullable
    private String completeConfiguredExecutableName(ExecutionEnvironment environment, String configuredExecutableName) {
        if (configuredExecutableName == null || configuredExecutableName.isEmpty()) {
            // guessProjectDir is not perfect. A project has modules. Modules have content roots. Content roots can be anywhere. The module configuration file (iml) can be anywhere.
            @Nullable String path = environment.getModulePath(); // often null
            VirtualFile preselectedExecutable = getPreselectedExecutable(environment, path);

            // FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor();
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                @Override
                public boolean isFileSelectable(@Nullable VirtualFile file) {
                    return isFileExecutable(file);
                }
            };
            Component parentComponent = null; // TODO
            FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, environment.getProject(), parentComponent);
            // TODO: Make it open a useful directory (for example PATH)
            VirtualFile[] selectedExecutables = chooser.choose(environment.getProject(), new VirtualFile[]{preselectedExecutable});
            //FileChooser.chooseFile(, environment.getProject(), preselectedExecutable);

            VirtualFile selectedExecutable = selectedExecutables.length > 0 ? selectedExecutables[0] : null;
            if (selectedExecutable != null) {
                configuredExecutableName = selectedExecutable.getPath();
            }
        }
        return configuredExecutableName;
    }

    private void loadExecutable(ExecutionEnvironment environment, String configuredExecutableName) {
        configuredExecutableName = completeConfiguredExecutableName(environment, configuredExecutableName);
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

    private void setUpGdb(ExecutionEnvironment environment) {
        ProjectSettingsState projectSettings = ProjectSettingsState.getInstance();
        try {
            gdbSet("mi-async", "on");
        } catch (GdbMiOperationException e) {
            reportError("mi-async on failed", e);
        }
        //gdbSet("interactive-mode", "on"); // just in case we use a pipe for communicating with gdb: force pty-like communication
        gdbSend("-enable-frame-filters", new String[]{}, new String[0]);
        try {
            gdbSet("sysroot", projectSettings.gdbSysRoot);
        } catch (GdbMiOperationException e) {
            reportError("Could not set sysroot to " + projectSettings.gdbSysRoot, e);
        }
        try {
            gdbSet("arch", projectSettings.gdbArch);
        } catch (GdbMiOperationException e) {
            reportError("Could not set arch to " + projectSettings.gdbArch, e);
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
        // gdbSend("-file-exec-and-symbols", new String[]{"/home/dannym/src/Oxide/main/amd-host-image-builder/target/debug/amd-host-image-builder"}, new String[0]);
        // TODO: -exec-arguments args
    }

    public DebugProcess(RunProfileState runProfileState, ExecutionEnvironment environment, Runner runner, XDebugSession session) throws IOException, ExecutionException {
        super(session);
        session.setPauseActionSupported(true);
        //session.setCurrentStackFrame();
        final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), runner);
        ExecutionConsole console = executionResult.getExecutionConsole();
        myProcessHandler = executionResult.getProcessHandler();
        myProcessHandler.putUserData(DEBUG_PROCESS_KEY, this);
        //PtyOnly pty = myProcessHandler.getUserData(RunProfileState.PTY);
        myExecutionConsole = executionResult.getExecutionConsole();
        myEditorsProvider = new EditorsProvider();
        myEnvironment = environment;
        myMiFilter = new GdbMiFilter(this, environment.getProject(), (GdbOsProcessHandler) myProcessHandler);

        Disposer.register(myExecutionConsole, this);
        //@Nullable OutputStream childIn = executionResult.getProcessHandler().getProcessInput();
        //myChildIn = childIn;

        // TODO: -file-list-exec-source-files, -file-list-shared-libraries, -file-list-symbol-files,
    }

    // We'll call initBreakpoints() at the right time on our own.
    @Override
    public boolean checkCanInitBreakpoints() {
        if (this.isGDBconnected) {
            reportError("Need to switch to SuspendContext before...");
        }
        return false;
    }

    @Override
    public XBreakpointHandler<?> /*@NotNull*/[] getBreakpointHandlers() {
        return myXBreakpointHandlers;
    }

    public BreakpointManager getBreakpointManager() {
        return myBreakpointManager;
    }

    @Nullable
//    public static DebugProcess getInstance(ProcessHandler handler) {
//        return handler.getUserData(DEBUG_PROCESS_KEY);
//    }

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
            gdbCall("-exec-until", new String[]{BreakpointManager.fileLineReference(position)}, new String[0]);
        } catch (RuntimeException e) {
//            e.printStackTrace();
//            final PsiFile psiFile = PsiManager.getInstance(getSession().getProject()).findFile(position.getFile());
//            assert psiFile != null;
//            StatusBar.Info.set(DebuggerBundle.message("status.bar.text.not.valid.position.in.file", psiFile.getName()), psiFile.getProject());
            //final Debugger c = myDebuggerSession.getClient();
            reportError("Cannot run to that position");
        } catch (GdbMiOperationException e) {
            reportError("Cannot run to that position", e);
        }
    }

    public void startDebugging() {
        isGDBconnected = true; // FIXME
        myMiFilter.startReaderThread();
        setUpGdb(myEnvironment);
        getSession().initBreakpoints();
        try {
            execRun();
        } catch (GdbMiOperationException e) {
            reportError("exec-run failed", e);
        }
    }

    public void processAsync(Optional<String> token, @NotNull Scanner scanner) {
        myMiFilter.processAsync(token, scanner);
    }
}
