package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.util.xml.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThrowedCatchpointProperties extends CppExceptionCatchpointProperties<ThrowedCatchpointProperties> {
    // FIXME attribute here
    //@Attribute("ExceptionRegexp")
    public ThrowedCatchpointProperties(String regexp)  {
        myExceptionRegexp = regexp;
    }
    @Override
    public String getExceptionBreakpointId() {
        return null; // FIXME
    }

    @Override
    public @Nullable ThrowedCatchpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ThrowedCatchpointProperties state) {
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
