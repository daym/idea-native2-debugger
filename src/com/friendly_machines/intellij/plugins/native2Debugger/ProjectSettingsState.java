// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

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
    public String gdbExecutableName = "gdb";
    public String gdbSysRoot = "target:";
    public String gdbArch = "auto";
    public String gdbTargetType = "exec";
    public String gdbTargetArg = null;
    public String symbolFile = null;

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
