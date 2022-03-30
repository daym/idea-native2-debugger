// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger;

// See ./java/execution/impl/src/com/intellij/execution/remote/RemoteConfigurationType.java

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.icons.AllIcons;

public class ConfigurationType extends SimpleConfigurationType implements DumbAware {
    public ConfigurationType() {
        super("Native2Debugger", "Native2Debugger", "Get a native executable to a debugger", // TODO: i18n
                NotNullLazyValue.createValue(() -> AllIcons.RunConfigurations.RemoteDebug)); // TODO: nicer icon
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new Configuration(project, this);
    }

    @NotNull
    @Override
    public String getTag() {
        return "native2debugger";
    }

    @Override
    public String getHelpTopic() {
        return "reference.dialogs.rundebug.native2debugger";
    }

    @NotNull
    @Deprecated(forRemoval = true)
    public ConfigurationFactory getFactory() {
        return this;
    }

    @NotNull
    public static ConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(ConfigurationType.class);
    }

    @Override
    public boolean isEditableInDumbMode() {
        return true;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
