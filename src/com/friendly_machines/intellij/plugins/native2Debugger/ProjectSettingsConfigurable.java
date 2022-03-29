package com.friendly_machines.intellij.plugins.native2Debugger;

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
        modified |= !mySettingsComponent.getGdbTargetText().equals(settings.gdbTarget);
//        modified |= mySettingsComponent.getIdeaUserStatus() != settings.ideaStatus;
        return modified;
    }

    @Override
    public void apply() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance();
        settings.gdbExecutableName = mySettingsComponent.getGdbExecutableNameText();
        settings.gdbSysRoot = mySettingsComponent.getGdbSysRootText();
        settings.gdbArch = mySettingsComponent.getGdbArchText();
        settings.gdbTarget = mySettingsComponent.getGdbTargetText();
    }

    @Override
    public void reset() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance();
        mySettingsComponent.setGdbExecutableNameText(settings.gdbExecutableName);
        mySettingsComponent.setGdbSysRootText(settings.gdbSysRoot);
        mySettingsComponent.setGdbArchText(settings.gdbArch);
        mySettingsComponent.setGdbTargetText(settings.gdbTarget);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}