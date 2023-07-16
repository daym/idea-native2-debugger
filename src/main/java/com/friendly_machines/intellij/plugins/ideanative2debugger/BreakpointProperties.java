package com.friendly_machines.intellij.plugins.ideanative2debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BreakpointProperties extends BreakpointPropertiesHelper<BreakpointProperties> {
    @Override
    public @Nullable BreakpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull BreakpointProperties state) {
        myHardware = state.myHardware;
    }
    @Override
    public void noStateLoaded() {
        super.noStateLoaded();
        myHardware = false;
    }

    public BreakpointProperties() {
    }
}
