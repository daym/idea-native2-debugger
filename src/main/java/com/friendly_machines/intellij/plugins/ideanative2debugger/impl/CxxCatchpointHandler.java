// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.friendly_machines.intellij.plugins.ideanative2debugger.CxxCatchpointProperties;
import com.friendly_machines.intellij.plugins.ideanative2debugger.CxxCatchpointType;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import org.jetbrains.annotations.NotNull;

public class CxxCatchpointHandler extends XBreakpointHandler<XBreakpoint<CxxCatchpointProperties>> {
    private final DebugProcess myDebugProcess;

    public CxxCatchpointHandler(DebugProcess debugProcess, final Class<? extends CxxCatchpointType> typeClass) {
        super(typeClass);
        myDebugProcess = debugProcess;
    }

    @Override
    public void registerBreakpoint(@NotNull XBreakpoint<CxxCatchpointProperties> breakpoint) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
        try {
            manager.addCxxCatchpoint(breakpoint);
        } catch (InterruptedException e) {
            // pucgenie: Can't really do much more.
            e.printStackTrace();
        }
    }

    /**
     * Called when a breakpoint need to be unregistered from the debugging engine
     *
     * @param key breakpoint to unregister
     * @param temporary  determines whether {@code breakpoint} is unregistered forever or it may be registered again. This parameter may
     *                   be used for performance purposes. For example the breakpoint may be disabled rather than removed in the debugging engine if
     *                   {@code temporary} is {@code true}
     */
    @Override
    public void unregisterBreakpoint(@NotNull XBreakpoint<CxxCatchpointProperties> key, final boolean temporary) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
        try {
            manager.deleteBreakpoint(key);
        } catch (InterruptedException e) {
            // pucgenie: Can't really do much more.
            e.printStackTrace();
        }
//        }
    }
}
