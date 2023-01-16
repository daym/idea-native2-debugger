// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.friendly_machines.intellij.plugins.ideanative2debugger.AdaCatchpointProperties;
import com.friendly_machines.intellij.plugins.ideanative2debugger.AdaCatchpointType;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.BreakpointManager;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import org.jetbrains.annotations.NotNull;

public class AdaCatchpointHandler extends XBreakpointHandler<XBreakpoint<AdaCatchpointProperties>> {
    private final DebugProcess myDebugProcess;

    public AdaCatchpointHandler(DebugProcess debugProcess, final Class<? extends AdaCatchpointType> typeClass) {
        super(typeClass);
        myDebugProcess = debugProcess;
    }

    @Override
    public void registerBreakpoint(@NotNull XBreakpoint<AdaCatchpointProperties> breakpoint) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
        try {
            manager.addAdaCatchpoint(breakpoint);
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
    public void unregisterBreakpoint(@NotNull XBreakpoint<AdaCatchpointProperties> key, final boolean temporary) {
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
