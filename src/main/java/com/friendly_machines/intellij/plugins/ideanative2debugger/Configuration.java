// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RemoteRunProfile;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;

//import javax.swing.*;

public class Configuration extends LocatableConfigurationBase
        implements RunConfigurationWithSuppressedDefaultRunAction, RemoteRunProfile /* TODO: Maybe remove RemoteRunProfile */ {


    private String[] myExecArguments = new String[0];
    private String myAttachTarget = null;

    protected Configuration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new ProjectDebugSettingsEditor(getProject());
        //SettingsEditorGroup<Configuration> group = new SettingsEditorGroup<>();
        // or just: return new Native2DebuggerSettingsEditor(getProject());
        //group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new RemoteConfigurable(getProject()));
        //group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
        //return group;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public @Nullable
    com.intellij.execution.configurations.RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        /*final GenericDebuggerRunnerSettings debuggerSettings = (GenericDebuggerRunnerSettings)env.getRunnerSettings();
        if (debuggerSettings != null) {
            // sync self state with execution environment's state if available
            debuggerSettings.LOCAL = false;
            debuggerSettings.setDebugPort(USE_SOCKET_TRANSPORT ? PORT : SHMEM_ADDRESS);
            debuggerSettings.setTransport(USE_SOCKET_TRANSPORT ? DebuggerSettings.SOCKET_TRANSPORT : DebuggerSettings.SHMEM_TRANSPORT);
        }*/
        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(getProject());
        return new RunProfileState(this, env, builder);
    }

    public void setExecArguments(String[] execArguments) {
        myExecArguments = execArguments;
    }

    public String[] getExecArguments() {
        return myExecArguments;
    }

    public @Nullable String getAttachTarget() {
        return myAttachTarget;
    }

    public void setAttachTarget(@Nullable String attachTarget) {
        myAttachTarget = attachTarget;
    }

    @Override
    public RunConfiguration clone() {
        final var result = (Configuration)super.clone();
        result.myExecArguments = new String[myExecArguments.length];
        System.arraycopy(myExecArguments, 0, result.myExecArguments, 0, myExecArguments.length);
        result.myAttachTarget = myAttachTarget;
        return result;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        //throw new RuntimeConfigurationError(XPathBundle.message("dialog.message.selected.xml.input.file.not.found"));
//        if (getEffectiveJDK() == null) {
//            throw new RuntimeConfigurationError(XPathBundle.message("dialog.message.no.jdk.available"));
//        }
    }
    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        // TODO Maybe use com.intellij.util.xmlb.XmlSerializer
        DefaultJDOMExternalizer.readExternal(this, element);

        final Element parameters = element.getChild("execArguments");
        if (parameters != null) {
            var execArguments = new ArrayList<String>();
            final var params = parameters.getChildren("param");
            for (var p : params) {
                execArguments.add(p.getText());
            }
            myExecArguments = execArguments.toArray(new String[0]);
        }
        var attachTarget = element.getChild("attachTarget");
        if (attachTarget != null) {
            myAttachTarget = attachTarget.getText();
        }

//        if (jdkChoice != null) {
//            myJdkChoice = JdkChoice.valueOf(jdkChoice.getAttributeValue("value"));
//        }
    }
    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        DefaultJDOMExternalizer.writeExternal(this, element);

        final Element params = new Element("execArguments");
        element.addContent(params);
        for (var arg : myExecArguments) {
            final Element p = new Element("param");
            params.addContent(p);
            p.setText(arg);
        }
        if (myAttachTarget != null) {
            var attachTarget = new Element("attachTarget");
            attachTarget.setText(myAttachTarget);
            element.addContent(attachTarget);
        }

//        final Element choice = new Element("JdkChoice");
//        choice.setAttribute("value", myJdkChoice.name());
//        element.addContent(choice);
    }

}
