package com.friendly_machines.intellij.plugins.ideanative2debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WatchpointProperties extends WatchpointPropertiesHelper<WatchpointProperties> {
    public WatchpointProperties(String expression, boolean watchReads, boolean watchWrites)  {
        myExpression = expression;
        myWatchReads = watchReads;
        myWatchWrites = watchWrites;
    }
    public WatchpointProperties() { // for serialization
    }

    @Override
    public @Nullable WatchpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull WatchpointProperties state) {
        myExpression = state.myExpression;
        myWatchReads = state.myWatchReads;
        myWatchWrites = state.myWatchWrites;
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
