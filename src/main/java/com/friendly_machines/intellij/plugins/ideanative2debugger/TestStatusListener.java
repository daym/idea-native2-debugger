package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestStatusListener extends SMTRunnerEventsAdapter {
//    @Override
//    public void onTestOutput(@NotNull SMTestProxy proxy, @NotNull TestOutputEvent event) {
//        super.onTestOutput(proxy, event);
//    }

    @Override
    public void onSuiteFinished(@NotNull SMTestProxy suite, @Nullable String nodeId) {
        super.onSuiteFinished(suite, nodeId);
        // Work around <https://github.com/intellij-rust/intellij-rust/issues/9463>.
        var testExecutableName = nodeId.replaceFirst("[)]$", "");
        System.err.println(testExecutableName);
        // TODO: Add temporary debug configuration for this test.
    }
}

// See CargoTestRunConfigurationProducerBase.kt
// See CargoTestRunConfigurationProducer.kt
