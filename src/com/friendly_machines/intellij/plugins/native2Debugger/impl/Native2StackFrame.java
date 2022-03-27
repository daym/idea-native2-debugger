// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class Native2StackFrame extends XStackFrame {
  private final HashMap<String, Object> myFrame;
  private final Native2DebugProcess myDebuggerSession;
  private final XSourcePosition myPosition;
  private final String myThreadId;

  @Nullable
  public static XSourcePosition createSourcePositionFromFrame(HashMap<String, Object> gdbFrame) {
    String file = (String) gdbFrame.get("fullname"); // TODO: or "file"--but that's relative
    // FIXME PsiFile
    VirtualFile p = VfsUtil.findFile(Path.of(file), false);
    String line = (String) gdbFrame.get("line");
    System.err.println("SOURCE POSITION FROM FRAME: " + p + ":" + line);
    return XDebuggerUtil.getInstance().createPosition(p, Integer.parseInt(line) - 1);
  }

  public Native2StackFrame(String threadId, HashMap<String, Object> gdbFrame, Native2DebugProcess debuggerSession) {
    myThreadId = threadId;
    myFrame = gdbFrame;
    myDebuggerSession = debuggerSession;
    myPosition = createSourcePositionFromFrame(gdbFrame);
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
    try {
      String file = (String) myFrame.get("file");
      String line = (String) myFrame.get("line");
      String func = myFrame.containsKey("func") ? (String) myFrame.get("func") : "";
      component.append(func, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      component.append(" at ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
      component.append(file + ":" + line, SimpleTextAttributes.LINK_ATTRIBUTES);
      // component.setIcon ?
      // TODO
    } catch (ClassCastException e) {
      component.append("failed to parse " + myFrame.toString(), SimpleTextAttributes.ERROR_ATTRIBUTES);
    }
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    try {
      String level = (String) myFrame.get("level");
      List<String> variables = myDebuggerSession.getVariables(myThreadId, level);
      // FIXME: handle result
    } catch (ClassCastException e) {
      e.printStackTrace();
    }
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
