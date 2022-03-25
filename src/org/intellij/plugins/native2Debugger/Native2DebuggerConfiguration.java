package org.intellij.plugins.native2Debugger;

import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

//import javax.swing.*;

// FIXME: not Java, not RemoteConfigurable

public class Native2DebuggerConfiguration extends LocatableConfigurationBase
        implements RunConfigurationWithSuppressedDefaultRunAction, RemoteRunProfile {


    protected Native2DebuggerConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        /* FIXME: extends SettingsEditor<XsltRunConfiguration>
  implements CheckableRunConfigurationEditor<XsltRunConfiguration>  */
        SettingsEditorGroup<Native2DebuggerConfiguration> group = new SettingsEditorGroup<>();
        // or just: return new Native2DebuggerSettingsEditor(getProject());
        //group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new RemoteConfigurable(getProject())); FIXME
        //group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
        return group;
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        /*final GenericDebuggerRunnerSettings debuggerSettings = (GenericDebuggerRunnerSettings)env.getRunnerSettings();
        if (debuggerSettings != null) {
            // sync self state with execution environment's state if available
            debuggerSettings.LOCAL = false;
            debuggerSettings.setDebugPort(USE_SOCKET_TRANSPORT ? PORT : SHMEM_ADDRESS);
            debuggerSettings.setTransport(USE_SOCKET_TRANSPORT ? DebuggerSettings.SOCKET_TRANSPORT : DebuggerSettings.SHMEM_TRANSPORT);
        }*/
        return new Native2DebuggerRunProfileState(this, env);
    }
}
