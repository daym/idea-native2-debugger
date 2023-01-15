package com.friendly_machines.intellij.plugins.ideanative2debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CxxExceptionCatchpointProperties2 extends CxxExceptionCatchpointProperties<CxxExceptionCatchpointProperties2> {
    public CxxExceptionCatchpointProperties2(String regexp)  {
        myExceptionRegexp = regexp;
    }

    @Override
    public @Nullable CxxExceptionCatchpointProperties2 getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CxxExceptionCatchpointProperties2 state) {
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
