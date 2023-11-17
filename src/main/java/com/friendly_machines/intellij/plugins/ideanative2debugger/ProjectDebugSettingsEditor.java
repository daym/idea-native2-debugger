package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.CheckableRunConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProjectDebugSettingsEditor extends SettingsEditor<Configuration> implements CheckableRunConfigurationEditor<Configuration> {
    private final Project myProject;
    private ProjectDebugSettingsEditorComponent myComponent;

    public ProjectDebugSettingsEditor(Project project) {
        myProject = project;
    }

    @Override
    protected void resetEditorFrom(@NotNull Configuration s) {
        myComponent.setExecArguments(s.getExecArguments());
    }

    @Override
    protected void applyEditorTo(@NotNull Configuration s) throws ConfigurationException {
        s.setExecArguments(myComponent.getExecArguments());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        myComponent = new ProjectDebugSettingsEditorComponent(myProject);
        return myComponent.getComponent();
    }

    @Override
    protected void disposeEditor() {
        super.disposeEditor();
        myComponent = null;
    }

    @Override
    public void checkEditorData(Configuration s) {
        // TODO maybe duplicate
    }
}
