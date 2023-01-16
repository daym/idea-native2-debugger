package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.ShlibCatchpointCatchType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShlibCatchpointProperties extends ShlibCatchpointPropertiesHelper<ShlibCatchpointProperties> {
    public ShlibCatchpointProperties(ShlibCatchpointCatchType catchType, String regexp)  {
        myCatchType = catchType;
        myLibraryNameRegexp = regexp;
    }
    public ShlibCatchpointProperties() { // for serialization
    }

    @Override
    public @Nullable ShlibCatchpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ShlibCatchpointProperties state) {
        myCatchType = state.myCatchType;
        myLibraryNameRegexp = state.myLibraryNameRegexp;
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
