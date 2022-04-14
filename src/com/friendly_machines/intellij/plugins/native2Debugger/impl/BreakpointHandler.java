// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.friendly_machines.intellij.plugins.native2Debugger.BreakpointType;
import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.BreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

public class BreakpointHandler extends XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> {
    private final DebugProcess myDebugProcess;

    public BreakpointHandler(DebugProcess debugProcess, final Class<? extends BreakpointType> typeClass) {
        super(typeClass);
        myDebugProcess = debugProcess;
    }

    @Override
    public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
        manager.addBreakpoint(breakpoint);
        //      final XDebugSession session = myNative2DebugProcess.getSession();
        //      session.reportMessage(Native2DebuggerBundle.message("notification.content.target.vm.not.responding.breakpoint.can.not.be.set"), MessageType.ERROR);
        //      session.setBreakpointInvalid(breakpoint, "Target VM is not responding. Breakpoint can not be set");
    }

    /**
     * Called when a breakpoint need to be unregistered from the debugging engine
     * @param breakpoint breakpoint to unregister
     * @param temporary determines whether {@code breakpoint} is unregistered forever or it may be registered again. This parameter may
     * be used for performance purposes. For example the breakpoint may be disabled rather than removed in the debugging engine if
     * {@code temporary} is {@code true}
     */
    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> key, final boolean temporary) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
//        if (temporary) {
//            Optional<Breakpoint> breakpointo = manager.getBreakpoint(key);
//            if (breakpointo.isPresent()) {
//                Breakpoint breakpoint = breakpointo.get();
//                breakpoint.setEnabled(false);
//            }
//        } else {
            manager.deleteBreakpoint(key);
//        }
    }
}
