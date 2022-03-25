package org.intellij.plugins.native2Debugger;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Native2DebuggerGdbMiFilter implements Filter {
    private final OSProcessHandler myOsProcessHandler;

    public Native2DebuggerGdbMiFilter(OSProcessHandler osProcessHandler) {
        myOsProcessHandler =  osProcessHandler;
    }
    @Override
    public @Nullable Result applyFilter(@NotNull String line, int i) {
        Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
        process.handleGdbMiLine(line);
        return null;
    }
}
