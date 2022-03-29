// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.friendly_machines.intellij.plugins.native2Debugger.BreakpointType;
import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.Breakpoint;
import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.BreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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

    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> key, final boolean temporary) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
        if (temporary) {
            Optional<Breakpoint> breakpointo = manager.getBreakpoint(key);
            if (breakpointo.isPresent()) {
                Breakpoint breakpoint = breakpointo.get();
                breakpoint.setEnabled(false);
            }
        } else {
            manager.deleteBreakpoint(key);
        }
    }
}
