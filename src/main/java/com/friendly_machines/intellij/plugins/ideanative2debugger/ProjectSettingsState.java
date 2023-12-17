// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "com.friendly_machines.intellij.plugins.ideanative2debugger.ProjectSettingsState",
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

    public List<String> prepareCommandLine() {
        var result = new ArrayList<String>();
        String gdbExecutableName = this.gdbExecutableName;
        if (gdbExecutableName == null || gdbExecutableName.equals("")) {
            gdbExecutableName = "gdb";
        }
        gdbExecutableName = PathEnvironmentVariableUtil.findExecutableInWindowsPath(gdbExecutableName);
        result.add(gdbExecutableName);
        result.add("--nw"); // no window
        result.add("-q");
        //commandLine.addParameter("-batch");
        //commandLine.addParameter("-return-child-result");
        // -d <sourcedir>
        // -s <symbols>
        // -cd=<dir>
        // -f (stack frame special format)
        // -tty=/dev/tty0

        result.add("--interpreter=mi3");
        // gdb needs either forward-slashes or doubly-escaped backslashes
        //result.add("--tty=" + slaveName.replace("\\", "\\\\"));
        //result.add("--eval-command=new-ui mi3 " + slaveName.replace("\\", "\\\\"));

        //commandLine.setWorkDirectory(workingDirectory);
        //charset = EncodingManager.getInstance().getDefaultCharset();
        return result;
    }
}
