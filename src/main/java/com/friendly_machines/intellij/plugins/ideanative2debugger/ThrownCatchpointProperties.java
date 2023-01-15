package com.friendly_machines.intellij.plugins.ideanative2debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThrownCatchpointProperties extends CxxExceptionCatchpointProperties<ThrownCatchpointProperties> {
    public ThrownCatchpointProperties(String regexp)  {
        myExceptionRegexp = regexp;
    }

    @Override
    public @Nullable ThrownCatchpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ThrownCatchpointProperties state) {
        myExceptionRegexp = state.myExceptionRegexp;
    }

    @Override
    public void noStateLoaded() {
        super.noStateLoaded();
    }

    @Override
    public void initializeComponent() {
        super.initializeComponent();
    }
}
