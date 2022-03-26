// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.*;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.Native2DebuggerSession;
import org.intellij.plugins.native2Debugger.VMPausedException;
import org.intellij.plugins.native2Debugger.rt.engine.DebuggerStoppedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;
import org.intellij.plugins.native2Debugger.rt.engine.Value;

import javax.swing.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

public class Native2StackFrame extends XStackFrame {
  private final HashMap<String, Object> myFrame;
  private final Native2DebuggerSession myDebuggerSession;
  private final XSourcePosition myPosition;

  public Native2StackFrame(HashMap<String, Object> gdbFrame, Native2DebuggerSession debuggerSession) {
    myFrame = gdbFrame;
    myDebuggerSession = debuggerSession;
    myPosition = Native2SourcePosition.createFromFrame(gdbFrame);
  }

  @Override
  public Object getEqualityObject() {
    return Native2StackFrame.class;
  }

  @Override
  public XDebuggerEvaluator getEvaluator() {
    //return myFrame instanceof Debugger.StyleFrame ? new MyEvaluator((Debugger.StyleFrame)myFrame) : null;
    return null;
  }

  @Override
  public XSourcePosition getSourcePosition() {
    return myPosition;
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    // TODO
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    // FIXME
//    try {
//      if (myFrame instanceof Debugger.StyleFrame) {
//        final List<Debugger.Variable> variables = ((Debugger.StyleFrame)myFrame).getVariables();
//        final XValueChildrenList list = new XValueChildrenList();
//        for (final Debugger.Variable variable : variables) {
//          list.add(variable.getName(), new MyValue(variable));
//        }
//        node.addChildren(list, true);
//      } else {
//        super.computeChildren(node);
//      }
//    } catch (VMPausedException ignored) {
//      node.setErrorMessage(Native2DebuggerBundle.message("dialog.message.target.vm.not.responding"));
//    }
  }

  //public Debugger.Frame getFrame() {
//    return myFrame;
//  }

}
