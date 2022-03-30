// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.friendly_machines.intellij.plugins.native2Debugger.StackFrame;
import com.friendly_machines.intellij.plugins.native2Debugger.Value;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class Evaluator extends XDebuggerEvaluator {
    private final StackFrame myFrame;
    private final DebugProcess mySession;

    @Override
    public void evaluate(@NotNull String s, @NotNull XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition xSourcePosition) {
        try {
            HashMap<String, Object> result = mySession.evaluate(s, myFrame.getThreadId(), myFrame.getLevel());
            String value = (String) result.get("value");
            xEvaluationCallback.evaluated(new Value("eval", value, false));
        } catch (GdbMiOperationException e) {
            xEvaluationCallback.errorOccurred(e.getDetails().getAttributes().toString());
        } catch (ClassCastException e) {
            xEvaluationCallback.errorOccurred("Could not evaluate " + s);
        }
    }

    public Evaluator(DebugProcess session, StackFrame frame) {
        mySession = session;
        myFrame = frame;
    }
}
