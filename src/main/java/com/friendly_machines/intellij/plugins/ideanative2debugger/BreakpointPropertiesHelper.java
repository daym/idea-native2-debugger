package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BreakpointPropertiesHelper<T> extends XBreakpointProperties<T> {
    @Attribute("hardware")
    public boolean myHardware;
}
