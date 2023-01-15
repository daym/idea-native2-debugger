package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.CxxCatchpointCatchType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CxxCatchpointProperties extends CxxCatchpointPropertiesHelper<CxxCatchpointProperties> {
    public CxxCatchpointProperties(CxxCatchpointCatchType catchType, String regexp)  {
        myCatchType = catchType;
        myExceptionRegexp = regexp;
    }

    @Override
    public @Nullable CxxCatchpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CxxCatchpointProperties state) {
        myCatchType = state.myCatchType;
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
