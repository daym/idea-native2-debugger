package com.friendly_machines.intellij.plugins.ideanative2debugger;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.intellij.openapi.util.NlsSafe;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import org.jetbrains.annotations.Nullable;

public abstract class CppExceptionCatchpointProperties<T> extends XBreakpointProperties<T> {
    @Attribute("exception")
    public String myExceptionRegexp;

    /** exception class name. FIXME: use regexp instead */
    @NlsSafe
    public String getException() {
        return myExceptionRegexp;
    }

    public abstract String getExceptionBreakpointId();

    public void setCondition(@Nullable String condition) {
    }

    public void setLogExpression(@Nullable String condition) {
    }
}
