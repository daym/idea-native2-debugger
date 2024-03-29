package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.AdaCatchpointCatchType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdaCatchpointProperties extends AdaCatchpointPropertiesHelper<AdaCatchpointProperties> {
    public AdaCatchpointProperties(AdaCatchpointCatchType catchType, String regexp, String condition)  {
        myCatchType = catchType;
        myException = regexp;
        myCondition = condition;
    }
    public AdaCatchpointProperties() { // for serialization
    }

    @Override
    public @Nullable AdaCatchpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AdaCatchpointProperties state) {
        myCatchType = state.myCatchType;
        myException = state.myException;
        myCondition = state.myCondition;
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
