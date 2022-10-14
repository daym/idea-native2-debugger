package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestStatusListener extends SMTRunnerEventsAdapter {
    private final Project myProject;
//    @Override
//    public void onTestOutput(@NotNull SMTestProxy proxy, @NotNull TestOutputEvent event) {
//        super.onTestOutput(proxy, event);
//    }

    @Override
    public void onSuiteFinished(@NotNull SMTestProxy suite, @Nullable String nodeId) {
        super.onSuiteFinished(suite, nodeId);
        // Work around <https://github.com/intellij-rust/intellij-rust/issues/9463>.
        var testExecutableName = nodeId.replaceFirst("[)]$", "");
        var projectDir = ProjectUtil.guessProjectDir(myProject);
        try {
            var testExecutable = projectDir.findChild("target").findChild("debug").findChild("deps").findChild(testExecutableName); // FIXME don't hardcode
            System.err.println(testExecutableName);
            System.err.println(testExecutable);
            // TODO: Add temporary debug configuration for this test.
            // Note: suite.myLocator: CargoTestLocator; suite.getLocator()
            //myProject.getService()
            var configurationFactory = ConfigurationType.getInstance().getFactory();
            var templateConfiguration = configurationFactory.createTemplateConfiguration(myProject);
            configurationFactory.createConfiguration("xxx" + testExecutableName, templateConfiguration);
        } catch (NullPointerException ex) {

        }
    }

    public TestStatusListener(Project project) {
        myProject = project;
    }
}

// See CargoTestRunConfigurationProducerBase.kt
// See CargoTestRunConfigurationProducer.kt
