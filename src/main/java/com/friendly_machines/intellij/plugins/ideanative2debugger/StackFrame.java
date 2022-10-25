// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.Evaluator;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.GdbMiOperationException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StackFrame extends XStackFrame {
    private final Map<String, ?> myFrame;
    private final DebugProcess myDebuggerSession;
    private final XSourcePosition myPosition;
    private final String myThreadId;

    @Nullable
    public XSourcePosition createSourcePositionFromFrame(Map<String, ?> gdbFrame) {
        VirtualFile p = null;
        if (gdbFrame.containsKey("fullname")) {
            String file = (String) gdbFrame.get("fullname"); // TODO: or "file"--but that's relative
            p = VfsUtil.findFile(Path.of(file), false);
        }
        if (p == null) {
            return null;
        }
//    if (p != null && gdbFrame.containsKey("file")) {
//      //String file = (String) gdbFrame.get("file");
//
//      final Project project = myDebuggerSession.getSession().getProject();
//      final PsiManager psiManager = PsiManager.getInstance(project);
//      final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
//      final PsiFile psiFile = psiManager.findFile(p);
//      if (psiFile != null) {
//        Document psiDocument = documentManager.getDocument(psiFile);
//        p = documentManager.getPsiFile(psiDocument).getVirtualFile();
//        //p = psiFile.getVirtualFile();
//      }
//    }
        var line = Optional.ofNullable((String) gdbFrame.get("line")).map(Integer::parseInt);
        if (line.isEmpty()) {
            return null;
        }
        return XDebuggerUtil.getInstance().createPosition(p, line.get() - 1);
    }

    public StackFrame(String threadId, Map<String, ?> gdbFrame, DebugProcess debuggerSession) {
        myThreadId = threadId;
        myFrame = gdbFrame;
        myDebuggerSession = debuggerSession;
        myPosition = createSourcePositionFromFrame(gdbFrame);
    }

    @Override
    public Object getEqualityObject() {
        return StackFrame.class;
    }

    @Override
    public XDebuggerEvaluator getEvaluator() {
        //return myFrame instanceof Debugger.StyleFrame ? new MyEvaluator((Debugger.StyleFrame)myFrame) : null;
        return new Evaluator(myDebuggerSession, this);
    }

    @Override
    public XSourcePosition getSourcePosition() {
        return myPosition;
    }

    @Override
    public void customizePresentation(@NotNull ColoredTextContainer component) {
        try {
            if (myFrame.containsKey("func")) {
                String func = (String) myFrame.get("func");
                component.append(func, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
            component.append(" at ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            if (myFrame.containsKey("file")) {
                String file = (String) myFrame.get("file");
                String line = myFrame.containsKey("line") ? (String) myFrame.get("line") : "?";
                component.append(file + ":" + line, SimpleTextAttributes.LINK_ATTRIBUTES);
            } else if (myFrame.containsKey("addr")) {
                component.append((String) myFrame.get("addr"), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
            // component.setIcon ?
            // TODO
        } catch (ClassCastException e) {
            component.append("failed to parse " + myFrame, SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        try {
            String level = (String) myFrame.get("level");
            var variables = (List<Map<String, String>>) (List<?>) myDebuggerSession.getVariables(myThreadId, level);
            final XValueChildrenList list = new XValueChildrenList();
            for (var variable : variables) {
                String name = variable.get("name");
                String value = variable.getOrDefault("value", "?");
                list.add(name, new Value(name, value, variable.containsKey("arg")));
            }
            node.addChildren(list, true);
        } catch (GdbMiOperationException e) {
            e.printStackTrace();
            myDebuggerSession.reportError("Failed evaluating variable", e);
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    public String getThreadId() {
        return myThreadId;
    }

    public String getLevel() {
        return (String) myFrame.get("level");
    }
}
