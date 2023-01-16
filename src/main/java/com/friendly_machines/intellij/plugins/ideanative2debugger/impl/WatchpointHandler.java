package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.friendly_machines.intellij.plugins.ideanative2debugger.WatchpointType;
import com.friendly_machines.intellij.plugins.ideanative2debugger.WatchpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import org.jetbrains.annotations.NotNull;

public class WatchpointHandler extends XBreakpointHandler<XBreakpoint<WatchpointProperties>> {
    private final DebugProcess myDebugProcess;

    public WatchpointHandler(DebugProcess debugProcess, final Class<? extends WatchpointType> typeClass) {
        super(typeClass);
        myDebugProcess = debugProcess;
    }

    @Override
    public void registerBreakpoint(@NotNull XBreakpoint<WatchpointProperties> breakpoint) {
        final BreakpointManager manager = myDebugProcess.getBreakpointManager();
        try {
            manager.addWatchpoint(breakpoint);
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
    public void unregisterBreakpoint(@NotNull XBreakpoint<WatchpointProperties> key, final boolean temporary) {
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
