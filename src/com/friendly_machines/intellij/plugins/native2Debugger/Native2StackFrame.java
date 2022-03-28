// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.friendly_machines.intellij.plugins.native2Debugger;

import com.friendly_machines.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import com.friendly_machines.intellij.plugins.native2Debugger.impl.Native2DebuggerGdbMiOperationException;
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
import java.util.Map;

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
      List<HashMap<String, Object>> variables = myDebuggerSession.getVariables(myThreadId, level);
      final XValueChildrenList list = new XValueChildrenList();
      for (HashMap<String, Object> variable: variables) {
        String name = (String) variable.get("name");
        // TODO: optional
        String value = variable.containsKey("value") ? (String) variable.get("value") : "?";
        list.add(name, new Native2Value(name, value, variable.containsKey("arg")));
      }
      node.addChildren(list, true);
    } catch (ClassCastException | Native2DebuggerGdbMiOperationException e) {
      e.printStackTrace();
    }
  }
}
