// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.native2Debugger;

import com.friendly_machines.intellij.plugins.native2Debugger.impl.DebugProcess;
import com.friendly_machines.intellij.plugins.native2Debugger.impl.GdbMiOperationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;

import java.util.*;

// Per thread
public class ExecutionStack extends XExecutionStack {
    //private final Native2StackFrame myTopFrame;
    private final DebugProcess myDebuggerSession;
    private final StackFrame myTopFrame;
    private final String myThreadId;

    public ExecutionStack(@NlsContexts.ListItem String name, String threadId, Optional<HashMap<String, Object>> topFrame, DebugProcess debuggerSession) {
        super(name);
        myDebuggerSession = debuggerSession;
        myThreadId = threadId;
//    for (Map.Entry<String, Object> frame : frames) {
//      if ("frame".equals(frame.getKey())) {
//        myFrames.add(new Native2StackFrame((HashMap<String, Object>) frame.getValue(), myDebuggerSession));
//      }
//    }
        if (topFrame.isPresent()) {
            myTopFrame = new StackFrame(threadId, topFrame.get(), myDebuggerSession);
        } else {
            myTopFrame = null;
        }
    }

    @Override
    public XStackFrame getTopFrame() {
        return myTopFrame;
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
//    if (myDebuggerSession.getCurrentState() == Debugger.State.SUSPENDED) {
        final List<XStackFrame> frames = new ArrayList<>();
        try {
            List<HashMap<String, Object>> gframes = myDebuggerSession.getFrames(myThreadId);
            for (HashMap<String, Object> gframe : gframes) {
                frames.add(new StackFrame(myThreadId, gframe, myDebuggerSession));
            }
        } catch (GdbMiOperationException e) {
            frames.add(myTopFrame);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (firstFrameIndex <= frames.size()) {
            container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
        } else {
            container.addStackFrames(Collections.emptyList(), true);
        }
//    }
    }
}
