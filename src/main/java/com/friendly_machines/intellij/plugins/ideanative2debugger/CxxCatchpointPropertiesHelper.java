package com.friendly_machines.intellij.plugins.ideanative2debugger;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.CxxCatchpointCatchType;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;

public abstract class CxxCatchpointPropertiesHelper<T> extends XBreakpointProperties<T> {
    @Attribute("catchType")
    public CxxCatchpointCatchType myCatchType;

    @Attribute("exception")
    public String myExceptionRegexp;


    /** exception class name. FIXME: use regexp instead */
    //@NlsSafe
    //public String getException() {
//        return myExceptionRegexp;
//    }

}
