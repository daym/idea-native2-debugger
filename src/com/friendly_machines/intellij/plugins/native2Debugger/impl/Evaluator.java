package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.friendly_machines.intellij.plugins.native2Debugger.Native2StackFrame;
import com.friendly_machines.intellij.plugins.native2Debugger.Native2Value;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Evaluator extends XDebuggerEvaluator {
    private final Native2StackFrame myFrame;
    private final DebugProcess mySession;

    @Override
    public void evaluate(@NotNull String s, @NotNull XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition xSourcePosition) {
        try {
            mySession.evaluate(s, myFrame.getThreadId(), myFrame.getLevel());
            xEvaluationCallback.evaluated(new Native2Value("eval", "value", false));
        } catch (GdbMiOperationException e) {
            e.printStackTrace();
            xEvaluationCallback.errorOccurred(e.toString());
        }
    }

    public Evaluator(DebugProcess session, Native2StackFrame frame) {
        mySession = session;
        myFrame = frame;
    }
}
