// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;

import java.util.*;

public class Native2ExecutionStack extends XExecutionStack {
  //private final Native2StackFrame myTopFrame;
  private final Native2DebugProcess myDebuggerSession;
  private final List<Native2StackFrame> myFrames = new ArrayList<>();

  public Native2ExecutionStack(@NlsContexts.ListItem String name, List<Map.Entry<String, Object>> frames, Native2DebugProcess debuggerSession) {
    super(name);
    myDebuggerSession = debuggerSession;
    for (Map.Entry<String, Object> frame : frames) {
      if ("frame".equals(frame.getKey())) {
        myFrames.add(new Native2StackFrame((HashMap<String, Object>) frame.getValue(), myDebuggerSession));
      }
    }
  }

  @Override
  public XStackFrame getTopFrame() {
    return myFrames.isEmpty() ? null : myFrames.get(0);
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    // TODO: be more lazy and compute them only here...

//    if (myDebuggerSession.getCurrentState() == Debugger.State.SUSPENDED) {
//      final List<XStackFrame> frames = new ArrayList<>();
//      frames.add(myTopFrame);
      List<Native2StackFrame> frames = myFrames;
      if (firstFrameIndex <= frames.size()) {
        container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
      } else {
        container.addStackFrames(Collections.emptyList(), true);
      }
//    }
  }
}
