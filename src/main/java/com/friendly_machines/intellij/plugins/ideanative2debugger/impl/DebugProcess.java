// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.friendly_machines.intellij.plugins.ideanative2debugger.*;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.notification.Notification;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.memory.component.InstancesTracker;
import com.intellij.xdebugger.memory.component.MemoryViewManager;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
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
// TODO: tracepoints, -trace-find, -trace-define-variable, -trace-frame-collected, -trace-list-variables, -trace-start, -trace-save
// TODO: public XValueMarkerProvider<?,?> createValueMarkerProvider(); If debugger values have unique ids just return these ids from getMarker(XValue) method. Alternatively implement markValue(XValue) to store a value in some registry and implement unmarkValue(XValue, Object) to remote it from the registry. In such a case the getMarker(XValue) method can return null if the value isn't marked.
// TODO: -info-os [processes]
// TODO: -list-thread-groups [--available] [--recurse 1] [group ...] and cache results
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

    private final BreakpointManager myBreakpointManager = new BreakpointManager(this);

    private final XBreakpointHandler<?>[] myXBreakpointHandlers = new XBreakpointHandler<?>[]{
            new BreakpointHandler(this, BreakpointType.class),
    };

    private GdbMiStateResponse gdbSend(String operation) throws IOException, InterruptedException {
        return myMiFilter.gdbSend(operation, Collections.emptyList(), Collections.emptyList());
    }

    private Map<String, ?> gdbCall(String operation, Collection<String> options, Collection<String> parameters) throws GdbMiOperationException, IOException, InterruptedException {
        return myMiFilter.gdbCall(operation, options, parameters);
    }

    private Map<String, ?> gdbCall(String operation, Collection<String> options) throws GdbMiOperationException, IOException, InterruptedException {
        return gdbCall(operation, options, Collections.emptyList());
    }

    private Map<String, ?> gdbCall(String operation, String singleOption) throws GdbMiOperationException, IOException, InterruptedException {
        return gdbCall(operation, List.of(singleOption));
    }

    private void handleGdbMiNotifyAsyncOutput(String klass, Map<String, ?> attributes) {
        if ((klass.equals("breakpoint-modified") || klass.equals("breakpoint-created") || klass.equals("breakpoint-deleted")) && attributes.containsKey("bkpt")) {
            // Note: if a breakpoint is emitted in the result record of a command, then it will not also be emitted in an async record.
            try {
                @SuppressWarnings("unchecked")
                var bkpt = (Map<String, ?>) attributes.get("bkpt");
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

    private void handleGdbMiExecAsyncOutput(String klass, Map<String, ?> attributes) throws IOException, InterruptedException {
        if (klass.equals("stopped")) {
            // TODO: running with thread-id (or "all"), stopped with thread-id or stopped (a list of ids or "all")
            // *stopped,reason="breakpoint-hit",disp="keep",bkptno="1",frame={addr="0x00007ffff7b53857",func="amd_host_image_builder::main",args=[],file="src/main.rs",fullname="/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs",line="2469",arch="i386:x86-64"},thread-id="1",stopped-threads="all",core="4"
            // Note: The point here is to change the IDEA debugger state to paused
            try {
                var reason = (String) attributes.get("reason");
//        String disp = (String) attributes.get("disp");
//        String bkptno = (String) attributes.get("bkptno");
//        String threadId = (String) attributes.get("thread-id");
//        String stoppedThreads = (String) attributes.get("stopped-threads");
//        String core = (String) attributes.get("core");
                if (reason != null && reason.startsWith("exited")) {
                    // TODO: reason=("exited-normally"|"exited"|"exited-signalled")
                    getSession().reportMessage("Debugged program exited with " + attributes, MessageType.INFO);
                    // Exit gdb when debugged program exits
                    this.stop();
                    return;
                }

                var tresponse = getThreadInfo();
                if (tresponse.containsKey("threads")) {
                    @SuppressWarnings("unchecked")
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

    public void handleGdbMiStateOutput(GdbMiStateResponse response) throws IOException, InterruptedException {
        // =breakpoint-modified{bkpt={number=1, times=0, original-location=/home/dannym/src/Oxide/main/amd-host-image-builder/src/main.rs:2472, locations=[{number=1.1, thread-groups=[i1], file=src/main.rs, func=amd_host_image_builder::main, line=2472, fullname=/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs, addr=0x00007ffff7b538d4, enabled=y}, {number=1.2, thread-groups=[i1], file=src/main.rs, func=amd_host_image_builder::main, line=2472, fullname=/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs, addr=0x00007ffff7b53a70, enabled=y}], type=breakpoint, addr=<MULTIPLE>, disp=keep, enabled=y}}
        char mode = response.getMode();
        String klass = response.getKlass();
        var attributes = response.getAttributes();
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
            {
                if (!text.isEmpty())
                    reportMessage(text, MessageType.INFO);
                break;
            }
            default:
                break;
        }
    }


    public void reportError(String s) {
        getSession().reportError(s);
    }

    public void reportMessage(@NotNull String text, @NotNull MessageType typ) {
        if (!Notification.isEmpty(text)) {
            getSession().reportMessage(text, typ);
        }
    }

    public void reportError(String s, GdbMiOperationException e) {
        GdbMiStateResponse details = e.getDetails();
        if (details != null) {
            var attributes = details.getAttributes();
            if (attributes != null) {
                Object msg = attributes.get("msg");
                if (msg != null) {
                    reportError(s + ": " + msg);
                    return;
                }
            }
        }
        reportError(s + ":" + e);
    }

    private static String getThreadName(Map<String, Object> thread, String id) {
        String name = thread.containsKey("target-id") ? (String) thread.get("target-id") : id;
        String state = thread.containsKey("state") ? (String) thread.get("state") : "";
        if (state.length() > 0) {
            name = name + ": " + state;
        }
        if (thread.containsKey("details")) {
            name = name + "; " + thread.get("details");
        }
        return name;
    }

    private SuspendContext generateSuspendContext(List<Object> threads, String currentThreadId) throws ClassCastException {
        final var stacks = new ArrayList<ExecutionStack>();
        int activeStackId = -1;

        for (Object thread1 : threads) {
            @SuppressWarnings("unchecked")
            var thread = (Map<String, Object>) thread1;
            String id = (String) thread.get("id");
            String name = getThreadName(thread, id);
            @SuppressWarnings("unchecked")
            var topFrame = (Map<String, Object>) thread.get("frame"); // can be null
            var stack = new ExecutionStack(name, id, topFrame, this); // one per thread
            stacks.add(stack);
            if (currentThreadId.equals(id)) {
                activeStackId = stacks.size() - 1;
            }
        }
        return new SuspendContext(this, stacks.toArray(new ExecutionStack[0]), activeStackId);
    }

    public List<Map<String, ?>> getVariables(String threadId, String frameId) throws GdbMiOperationException, IOException, InterruptedException {
        // TODO: --simple-values and find stuff yourself.
        var q = gdbCall("-stack-list-variables", List.of("--thread", threadId, "--frame", frameId, "--all-values"));
        var variables = (List<Map<String, ?>>) q.get("variables");
        if (variables == null) {
            return Collections.emptyList();
        }
        return variables;
    }

    public List<Map<String, ?>> getFrames(String threadId) throws GdbMiOperationException, ClassCastException, IOException, InterruptedException {
        var q = gdbCall("-stack-list-frames", List.of("--thread", threadId));
        final var result = new ArrayList<Map<String, ?>>();

        var stack = (Collection<Map.Entry<String, Map<String, ?>>>) q.get("stack");
        if (stack == null) {
            reportError("could not get stack frames of thread");
            return Collections.emptyList();
        }
        for (var frame : stack) {
            if ("frame".equals(frame.getKey())) {
                result.add(frame.getValue());
            }
        }
        return result;
    }

    private Map<String, ?> getThreadInfo() throws GdbMiOperationException, IOException, InterruptedException { // TODO return type ?
        return gdbCall("-thread-info", Collections.emptyList());
    }

    private void loadSymbols(String filename) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-file-symbol-file", filename);
    }

    private void gdbTarget(String gdbTargetType, String gdbTargetArg) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-target-select", List.of(gdbTargetType, gdbTargetArg));
    }

    private void gdbTarget(String gdbTargetType) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-target-select", gdbTargetType);
    }

    private void gdbSet(String key, String value) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-gdb-set", List.of(key, value));
    }
    private Object gdbShow(String key) throws GdbMiOperationException, IOException, InterruptedException {
        var result = gdbCall("-gdb-show", key);
        return result.get("value");
    }

    public Map<String, ?> dprintfInsert(Collection<String> options, Collection<String> parameters) throws GdbMiOperationException, IOException, InterruptedException {
        return gdbCall("-dprintf-insert", options, parameters);
    }

    public Map<String, ?> breakInsert(Collection<String> options, Collection<String> parameters) throws GdbMiOperationException, IOException, InterruptedException {
        return gdbCall("-break-insert", options, parameters);
    }

    public void breakDelete(String number) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-break-delete", number);
    }

    public void breakEnable(String number) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-break-enable", number);
    }

    public void breakDisable(String number) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-break-disable", number);
    }

    public Map<String, ?> evaluate(String expr, String threadId, String frameId) throws GdbMiOperationException, IOException, InterruptedException {
        return gdbCall("-data-evaluate-expression", List.of("--thread", threadId, "--frame", frameId, expr));
    }

    private void execRun() throws GdbMiOperationException, IOException, InterruptedException {
        System.err.println("EXEC RUN");
        gdbCall("-exec-run", List.of("--start")); // FIXME optional "--start"
    }

    private static boolean isFileExecutable(VirtualFile file) {
        if (file == null) {
            return false;
        }
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
            var f = new File(file.getPath());
            return f.canExecute();
        } else {
            return true; // TODO
        }
    }

    /// Note: Prefers files in shallow nesting level
    private @Nullable VirtualFile selectGoodFile(VirtualFile base, int fuel) {
        for (var child : base.getChildren()) {
            if (!child.isDirectory() && isFileExecutable(child) && child.getLength() > 0) {
                return child;
            }
        }

        if (fuel > 0) {
            for (var child : base.getChildren()) {
                if (child.isDirectory() && !child.is(VFileProperty.SYMLINK)) {
                        return selectGoodFile(child, fuel - 1);
                }
            }
        }
        return null;
    }

    @Nullable
    private VirtualFile getPreselectedExecutable(ExecutionEnvironment environment, @Nullable String path) {
        @Nullable VirtualFile base = path != null ? LocalFileSystem.getInstance().findFileByPath(path) : null;
        if (base == null) {
            base = ProjectUtil.guessProjectDir(environment.getProject());
        }
        if (base != null) {
            if (base.findFileByRelativePath("target") != null) { // Rust
                base = base.findFileByRelativePath("target");
            }
            if (base == null) {
                return null;
            }
            return selectGoodFile(base, 1000);
        }
        return null;
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
            descriptor.setTitle("Please select the executable to be debugged");
            Component parentComponent = null; // TODO
            FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, environment.getProject(), parentComponent);
            // TODO: Make it open a useful directory (for example PATH)
            VirtualFile[] selectedExecutables = chooser.choose(environment.getProject(), preselectedExecutable);
            //FileChooser.chooseFile(, environment.getProject(), preselectedExecutable);

            VirtualFile selectedExecutable = selectedExecutables.length > 0 ? selectedExecutables[0] : null;
            if (selectedExecutable != null) {
                configuredExecutableName = selectedExecutable.getPath();
            }
        }
        return configuredExecutableName;
    }

    private void loadExecutable(ExecutionEnvironment environment, String configuredExecutableName) throws IOException, InterruptedException {
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

    private void setUpGdb(ExecutionEnvironment environment) throws IOException, InterruptedException {
        ProjectSettingsState projectSettings = ProjectSettingsState.getInstance();
        try {
            gdbSet("mi-async", "on");
        } catch (GdbMiOperationException e) {
            reportError("mi-async on failed", e);
        }
        //gdbSet("interactive-mode", "on"); // just in case we use a pipe for communicating with gdb: force pty-like communication
        gdbSend("-enable-frame-filters");
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
            }
        } catch (GdbMiOperationException e) {
            reportError("Loading symbols failed", e);
        }
        try {
            reportMessage(listFeatures().toString(), MessageType.INFO);
        } catch (GdbMiOperationException e) {
            e.printStackTrace();
        }
//        try {
//            reportMessage(infoGdbMiCommand("quux").toString(), MessageType.INFO);
//        } catch (GdbMiOperationException e) {
//            e.printStackTrace();
//        }
        // gdbSend("-file-exec-and-symbols", new String[]{"/home/dannym/src/Oxide/main/amd-host-image-builder/target/debug/amd-host-image-builder"}, new String[0]);
        // TODO: -exec-arguments args
    }

    public DebugProcess(RunProfileState runProfileState, ExecutionEnvironment environment, Runner runner, XDebugSession session) throws IOException, ExecutionException {
        super(session);
        session.setPauseActionSupported(true);
        //session.setCurrentStackFrame();
        final ExecutionResult executionResult = runProfileState.execute(environment.getExecutor(), runner);
        //ExecutionConsole console = executionResult.getExecutionConsole();
        myProcessHandler = executionResult.getProcessHandler();
        myProcessHandler.putUserData(DEBUG_PROCESS_KEY, this);
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
    public XBreakpointHandler<?> @NotNull[] getBreakpointHandlers() {
        return myXBreakpointHandlers;
    }

    public BreakpointManager getBreakpointManager() {
        return myBreakpointManager;
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

    public void step(boolean reverse) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-step", reverse ? List.of("--reverse") : Collections.emptyList());
    }
    public void next(boolean reverse) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-next", reverse ? List.of("--reverse") : Collections.emptyList());
    }
    public void stepInstruction(boolean reverse) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-step-instruction", reverse ? List.of("--reverse") : Collections.emptyList());
    }
    public void nextInstruction(boolean reverse) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-next-instruction", reverse ? List.of("--reverse") : Collections.emptyList());
    }

    public void finish(boolean reverse) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-finish", reverse ? List.of("--reverse") : Collections.emptyList());
    }
    public void until(Optional<String> location) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-until", location.map(List::of).orElse(Collections.emptyList()));
    }
    public void jump(String location) throws GdbMiOperationException, IOException, InterruptedException {
        gdbCall("-exec-jump", List.of(location));
    }
    public List<String> listFeatures() throws GdbMiOperationException, ClassCastException, IOException, InterruptedException {
        // For example, GDB 12.1 has ^done,features=["frozen-varobjs","pending-breakpoints","thread-info","data-read-memory-bytes","breakpoint-notifications","ada-task-info","language-option","info-gdb-mi-command","undefined-command-error-code","exec-run-start-option","data-disassemble-a-option","python"]
        return (List<String>) gdbCall("-list-features", Collections.emptyList()).get("features");
    }

    /**
     *
     * https://github.com/daym/idea-native2-debugger/pull/6#discussion_r1002783308
     * @param commandName
     * @return Map{"exists":true/false}
     * @throws GdbMiOperationException
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<String, ?> infoGdbMiCommand(String commandName) throws GdbMiOperationException, IOException, InterruptedException {
        return ((Map<String, Map<String, ?>>) gdbCall("-info-gdb-mi-command", commandName)).get("command");
    }
    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        try {
            next(false);
        } catch (GdbMiOperationException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // pucgenie: Most probably interrupted by the IDE.
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        try {
            step(false);
        } catch (GdbMiOperationException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // pucgenie: Most probably interrupted by the IDE.
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        try {
            finish(false);
        } catch (GdbMiOperationException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // pucgenie: Most probably interrupted by the IDE.
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void startPausing() {
        try {
            gdbSend("-exec-interrupt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // pucgenie: Most probably interrupted by the IDE.
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
        //getSession().pause();
    }

    public Object dataReadMemoryBytes(int byteOffset, String addressExpr, int countBytes) throws GdbMiOperationException, IOException, InterruptedException {
        return gdbCall("-data-read-memory-bytes", List.of( "-o", Integer.toString(byteOffset), addressExpr, Integer.toString(countBytes) ));
    }

    private final static char[] hexdigits = "0123456789abcdef".toCharArray();

    public Object dataWriteMemoryBytes(String addressExpr, byte[] contents) throws GdbMiOperationException, IOException, InterruptedException {
        var contentsStream = new StringBuilder();
        for (byte item : contents) {
            contentsStream.append(hexdigits[(item >> 4) & 0xF]);
            contentsStream.append(hexdigits[(item >> 0) & 0xF]);
        }
        return gdbCall("-data-write-memory-bytes", List.of( addressExpr, contentsStream.toString() ));
    }
    @SuppressWarnings("unchecked")
    public List<String> dataListChangedRegisters() throws GdbMiOperationException, ClassCastException, IOException, InterruptedException {
        var result = gdbCall("-data-list-changed-registers", Collections.emptyList());
        // ^done,changed-registers=[...]
        if (result.containsKey("changed-registers")) {
            return (List<String>) result.get("changed-registers");
        } else {
            throw new RuntimeException("invalid result");
        }
    }
    @SuppressWarnings("unchecked")
    public List<String> dataListRegisterNames() throws GdbMiOperationException, ClassCastException, IOException, InterruptedException {
        var result = gdbCall("-data-list-register-names", Collections.emptyList());
        if (result.containsKey("register-names")) {
            return (List<String>) result.get("register-names");
        } else {
            throw new RuntimeException("invalid result");
        }
    }

    // TODO: Arg: list of registers
    @SuppressWarnings("unchecked")
    public List<Map<String, ?>> dataListRegisterValues(String fmt) throws GdbMiOperationException, ClassCastException, IOException, InterruptedException {
        var result = gdbCall("-data-list-register-values", fmt);
        if (result.containsKey("register-values")) {
            return (List<Map<String, ?>>) result.get("register-values");
        } else {
            throw new RuntimeException("invalid dataListRegisterValues result");
        }
    }

    public Map<String, List<Map.Entry<String, ?>>> dataDisassemble(String beginningAddress, String endAddress, GdbMiDisassemblyMode mode) throws GdbMiOperationException, IOException, InterruptedException {
        return (Map<String, List<Map.Entry<String, ?>>>) gdbCall("-data-disassemble", List.of( "-s", beginningAddress, "-e", endAddress ), List.of( Integer.toString(mode.code()) ));
    }

    // FIXME: allow specifying endAddress
    public Map<String, ?> dataDisassembleFile(String filename, int linenum, Optional<Integer> lineCount, boolean includeHighlevelSource) throws GdbMiOperationException, IOException, InterruptedException {
        var options = new ArrayList<String>();
        options.add("-f");
        options.add(filename);

        options.add("-l");
        options.add(Integer.toString(linenum));
        lineCount.ifPresent(x -> {
            options.add("-n");
            options.add(Integer.toString(x));
        });

        return gdbCall("-data-list-register-values", options, List.of( includeHighlevelSource ? "1" : "0" ));
    }

    @Override
    public void stop() {
        // Note: IDEA usually calls this AFTER the process was already terminated.
        if (!myProcessHandler.isProcessTerminated()) {
            try {
                gdbSend("-gdb-exit");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                // pucgenie: Most probably interrupted by the IDE.
                //throw new RuntimeException(e);
            }
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
        try {
            gdbSend("-exec-continue");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // pucgenie: Most probably interrupted by the IDE.
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
        try {
            until(Optional.of(BreakpointManager.fileLineReference(position)));
        } catch (RuntimeException | IOException e) {
//            e.printStackTrace();
//            final PsiFile psiFile = PsiManager.getInstance(getSession().getProject()).findFile(position.getFile());
//            assert psiFile != null;
//            StatusBar.Info.set(DebuggerBundle.message("status.bar.text.not.valid.position.in.file", psiFile.getName()), psiFile.getProject());
            //final Debugger c = myDebuggerSession.getClient();
            reportError("Cannot run to that position");
        } catch (GdbMiOperationException e) {
            reportError("Cannot run to that position", e);
        } catch (InterruptedException e) {
            // pucgenie: Most probably interrupted by the IDE.
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    public void startDebugging() throws IOException, InterruptedException {
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

    public void processAsync(Optional<String> token, @NotNull Scanner scanner) throws IOException, InterruptedException {
        myMiFilter.processAsync(token, scanner);
    }

    private void registerMemoryViewPanel(@NotNull RunnerLayoutUi ui) {
        if (!Registry.is("debugger.enable.memory.view"))
            return;

        final XDebugSession session = getSession();
        final InstancesTracker tracker = InstancesTracker.getInstance(getSession().getProject());
        final MemoryView memoryView = new MemoryView(session, this, tracker);
        final Content content = ui.createContent(MemoryViewManager.MEMORY_VIEW_CONTENT, memoryView, DebuggerBundle.message("memory.toolwindow.title"), null, memoryView.getDefaultFocusedComponent());
        content.setCloseable(false);
        content.setShouldDisposeContent(true);
        ui.addContent(content, 0, PlaceInGrid.right, true);
        // FIXME: memoryView.setActive(content.isSelected());
        //final DebuggerManagerThreadImpl managerThread = process.getManagerThread();
        ui.addListener(new ContentManagerListener() {
            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                if (event.getContent() == content) {
                    memoryView.setActive(content.isSelected());
                }
            }
        }, content);
    }

    private void registerAssemblyViewPanel(@NotNull RunnerLayoutUi ui) {
        final XDebugSession session = getSession();
        final CpuAssemblyView view = new CpuAssemblyView(session, this);
        final Content content = ui.createContent("AssemblyView", view, DebuggerBundle.message("assembly.toolwindow.title"), null, view.getDefaultFocusedComponent());
        content.setCloseable(false);
        content.setShouldDisposeContent(true);
        ui.addContent(content, 0, PlaceInGrid.right, true);
        //final DebuggerManagerThreadImpl managerThread = process.getManagerThread();
        ui.addListener(new ContentManagerListener() {

            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                if (event.getContent() == content) {
                    view.setActive(content.isSelected());
                }
            }
        }, content);
    }

    private void registerCpuRegistersViewPanel(@NotNull RunnerLayoutUi ui) {
        final XDebugSession session = getSession();
        final var view = new CpuRegistersView(session, this);
        final var content = ui.createContent("RegistersView", view, DebuggerBundle.message("registers.toolwindow.title"), null, view.getDefaultFocusedComponent());
        content.setCloseable(false);
        content.setShouldDisposeContent(true);
        ui.addContent(content, 0, PlaceInGrid.right, true);
        // FIXME: view.setActive(content.isSelected());
        //final DebuggerManagerThreadImpl managerThread = process.getManagerThread();
        ui.addListener(new ContentManagerListener() {
            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                if (event.getContent() == content) {
                    view.setActive(content.isSelected());
                }
            }
        }, content);
    }

    @NotNull
    @Override
    public XDebugTabLayouter createTabLayouter() {
        return new XDebugTabLayouter() {
            @Override
            public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
                registerMemoryViewPanel(ui);
                registerAssemblyViewPanel(ui);
                registerCpuRegistersViewPanel(ui);
            }
        };
    }

    @Override
    public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar, @NotNull DefaultActionGroup settings) {
        super.registerAdditionalActions(leftToolbar, topToolbar, settings);
        //     settings.add(new WatchReturnValuesAction(this));
        //    settings.add(new PyVariableViewSettings.SimplifiedView(this));
        //    settings.add(new PyVariableViewSettings.VariablesPolicyGroup());
        //leftToolbar.add(Separator.getInstance());
        //leftToolbar.add(Separator.getInstance());
        // TODO: ToggleAction

    }
}
