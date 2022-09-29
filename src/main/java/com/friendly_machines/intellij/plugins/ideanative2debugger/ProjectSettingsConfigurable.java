// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProjectSettingsConfigurable implements Configurable {
    private ProjectSettingsComponent mySettingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Native2 Debugger";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new ProjectSettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance();
        boolean modified = false;
        modified |= !mySettingsComponent.getGdbExecutableNameText().equals(settings.gdbExecutableName);
        modified |= !mySettingsComponent.getGdbSysRootText().equals(settings.gdbSysRoot);
        modified |= !mySettingsComponent.getGdbArchText().equals(settings.gdbArch);
        modified |= !mySettingsComponent.getGdbTargetTypeText().equals(settings.gdbTargetType);
        modified |= !mySettingsComponent.getGdbTargetArgText().equals(settings.gdbTargetArg);
        modified |= !mySettingsComponent.getSymbolFile().equals(settings.symbolFile);
//        modified |= mySettingsComponent.getIdeaUserStatus() != settings.ideaStatus;
        return modified;
    }

    @Override
    public void apply() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance();
        settings.gdbExecutableName = mySettingsComponent.getGdbExecutableNameText();
        settings.gdbSysRoot = mySettingsComponent.getGdbSysRootText();
        settings.gdbArch = mySettingsComponent.getGdbArchText();
        settings.gdbTargetType = mySettingsComponent.getGdbTargetTypeText();
        settings.gdbTargetArg = mySettingsComponent.getGdbTargetArgText();
        settings.symbolFile = mySettingsComponent.getSymbolFileText();
    }

    @Override
    public void reset() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance();
        mySettingsComponent.setGdbExecutableNameText(settings.gdbExecutableName);
        mySettingsComponent.setGdbSysRootText(settings.gdbSysRoot);
        mySettingsComponent.setGdbArchText(settings.gdbArch);
        mySettingsComponent.setGdbTargetTypeText(settings.gdbTargetType);
        mySettingsComponent.setGdbTargetArgText(settings.gdbTargetArg);
        mySettingsComponent.setSymbolFileText(settings.symbolFile);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}
