package com.friendly_machines.intellij.plugins.native2Debugger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "com.friendly_machines.intellij.plugins.native2Debugger.ProjectSettingsState",
        storages = @Storage("Native2DebuggerPlugin.xml")
)
public class ProjectSettingsState implements PersistentStateComponent<ProjectSettingsState> {
    // TODO: just add public fields here
    public String gdbExecutableName;

    public static ProjectSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(ProjectSettingsState.class);
    }

    @Nullable
    @Override
    public ProjectSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ProjectSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
