package org.intellij.plugins.native2Debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.util.Key;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import java.util.List;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.DefaultExecutionResult;

public class Native2DebuggerRunProfileState extends CommandLineState {
    public static final Key<Native2DebuggerRunProfileState> STATE = Key.create("STATE");
    private final Native2DebuggerConfiguration myConfiguration;
    private final TextConsoleBuilder myBuilder;

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
        GeneralCommandLine commandLine = new GeneralCommandLine(PathEnvironmentVariableUtil.findExecutableInWindowsPath("gdb"));
        commandLine.addParameter("-q");
        // -d <sourcedir>
        // -s <symbols>
        // -cd=<dir>
        // -f (stack frame special format)
        // -tty=/dev/tty0
        commandLine.addParameter("--interpreter=mi3");
        //commandLine.addParameter("--args");
        //commandLine.addParameter("./target/debug/amd-host-image-builder"); // FIXME
        //commandLine.setWorkDirectory(workingDirectory);
        //charset = EncodingManager.getInstance().getDefaultCharset();
        //final OSProcessHandler processHandler = creator.fun(commandLine);

        final OSProcessHandler osProcessHandler = new OSProcessHandler(commandLine);
        osProcessHandler.putUserData(STATE, this);

        // This assumes that we can do that still and have it have an effect. That's why we override execute() to make sure that that's the case.
        myBuilder.addFilter(new Native2DebuggerGdbMiFilter(osProcessHandler));
        this.setConsoleBuilder(myBuilder);

        // FIXME: osProcessHandler.addProcessListener(new MyProcessAdapter());
        // "Since we cannot guarantee that the listener is added before process handled is start notified, ..." ugh

        /* FIXME final List<XsltRunnerExtension> extensions = XsltRunnerExtension.getExtensions(myXsltRunConfiguration, myIsDebugger);
        for (XsltRunnerExtension extension : extensions) {
            osProcessHandler.addProcessListener(extension.createProcessListener(myXsltRunConfiguration.getProject(), myExtensionData));
        }
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (outputTypeFilter.value(outputType)) {
          final String text = event.getText();
          outputBuilder.append(text);
          LOG.debug(text);
        }
      }
    });
    processHandler.startNotify();
    if (!processHandler.waitFor(timeout)) {
      throw new ExecutionException(IdeUtilIoBundle.message("script.execution.timeout", String.valueOf(timeout / 1000)));
    }
    return outputBuilder.toString();

        */
        return osProcessHandler;
    }

    /* ProcessTerminatedListener.attach(process);
./python/src/com/jetbrains/pyqt/CompileQrcAction.java-      new RunContentExecutor(project, process)
./python/src/com/jetbrains/pyqt/CompileQrcAction.java-        .withTitle(PyBundle.message("qt.run.tab.title.compile.qrc"))
./python/src/com/jetbrains/pyqt/CompileQrcAction.java-        .run();
 */
    /* ./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-  public static String getProcessOutput(@NotNull GeneralCommandLine commandLine, @NotNull Condition<? super Key> outputTypeFilter, long timeout)
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-    throws ExecutionException {
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java:    return getProcessOutput(new OSProcessHandler(commandLine), outputTypeFilter,
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-                            timeout);
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-  }
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java:  public static OSProcessHandler execute(@NotNull String exePath,
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-                                         @Nullable String workingDirectory,
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-                                         @Nullable VirtualFile scriptFile,
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-                                         String[] parameters,
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java-                                         @Nullable Charset charset,
./platform/platform-util-io/src/com/intellij/execution/process/ScriptRunnerUtil.java:                                         @NotNull ThrowableNotNullFunction<? super GeneralCommandLine, ? extends OSProcessHandler, ? extends ExecutionException> creator)

*/

}
